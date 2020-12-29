package com.github.oskin1.factnet.network

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.WallClock.AlwaysIncreasingClock.currentTimeMillis
import com.github.oskin1.factnet.config.NetworkConfig
import com.github.oskin1.factnet.domain.Tag
import com.github.oskin1.factnet.network.NetworkController._
import com.github.oskin1.factnet.network.RemotePeerHandler.SendHandshake
import com.github.oskin1.factnet.network.domain.RequestId
import com.github.oskin1.factnet.services.FactsService.{Add, Get, SearchResult}
import scorex.util.encode.Base16

import scala.util.Random

final class NetworkController(
  config: NetworkConfig,
  factsServiceRef: ActorRef
) extends Actor
  with ActorLogging {

  import context.system

  private val tcpManager = IO(Tcp)

  private var connections     = PeerStore.empty(config.maxConnections)
  private var handlers        = Map.empty[InetSocketAddress, ActorRef]
  private var pendingRequests = Map.empty[RequestId, Option[InetSocketAddress]]

  override def preStart(): Unit =
    tcpManager ! Bind(self, config.bindAddress)

  def receive: Receive = binding

  private def binding: Receive = {
    case Bound(localAddress) =>
      log.info(s"Bound to [$localAddress]")
      // 1. bootstrap
      connectTo(config.knownPeers)
      context become working
    case CommandFailed(_: Bind) =>
      log.error(s"Failed to bind to [${config.bindAddress}]")
      context stop self
      sys.exit(1)
  }

  private def working: Receive =
    commands orElse remoteConnections orElse search orElse logUnhandled

  private def logUnhandled: Receive = {
    case m =>
      log.info(s"Got unhandled message $m")
  }

  private def commands: Receive = {
    case ConnectTo(remoteAddress) =>
      if (connections.contains(remoteAddress))
        log.warning(s"A connection to [$remoteAddress] already established")
      else {
        log.info(s"Connecting to [$remoteAddress]")
        tcpManager ! Connect(
          remoteAddress = remoteAddress,
          options       = Nil,
          timeout       = Some(config.connectionTimeout),
          pullMode      = true
        )
      }
  }

  private def remoteConnections: Receive = {
    case Connected(remoteAddress, localAddress) =>
      log.info(s"Successfully connected to [$localAddress -> $remoteAddress]")
      connections.add(remoteAddress, currentTimeMillis()) match {
        case Right(updatedPeerStore) =>
          connections = updatedPeerStore
          val connection   = sender()
          val handlerProps = RemotePeerHandler.props(connection, self, remoteAddress)
          val handler      = context.actorOf(handlerProps, s"RemotePeerHandler")
          context.watch(handler)
          handler ! SendHandshake(config.localName)
        case Left(err) => log.warning(s"Failed to create new connection. $err")
      }
    case cf @ CommandFailed(Connect(remoteAddress, _, _, _, _)) =>
      log.warning(s"Failed to connect to [$remoteAddress]. ${cf.cause.map(_.getMessage).getOrElse("?")}")
      // 1. drop failed connection
      connections = connections.remove(remoteAddress)
    case ConnectionLost(remoteAddress) =>
      // 1. remove corresponding handler from the registry
      handlers -= remoteAddress
      // 2. drop lost connection
      connections = connections.remove(remoteAddress)
    case MessageFrom(remoteAddress, message, seenAt) =>
      // 1. update timestamp `remoteAddress` was last seen
      connections = connections.seen(remoteAddress, seenAt)
      // 2. handle concrete message
      handle(message, remoteAddress, sender())
    case Handshaked(remoteAddress, ts) =>
      log.info(s"A connection to [$remoteAddress] confirmed")
      // 1. confirm connection to `remoteAddress`
      connections = connections.confirm(remoteAddress, ts)
      handlers += remoteAddress -> sender()
      // 2. discover more peers if needed
      val neededConnection = config.minConnections - connections.size
      if (neededConnection > 0) sender() ! GetPeers(neededConnection)
  }

  private def search: Receive = {
    case SearchResult(facts, Some(requestId)) =>
      if (facts.nonEmpty) {
        pendingRequests.get(requestId) match {
          case Some(Some(requesterAddress)) =>
            handlers.get(requesterAddress).fold(log.warning(s"Handler for [$requesterAddress] not found")) { handlerRef =>
              handlerRef ! Facts(requestId, facts)
            }
          case _ =>
        }
      }
    case SearchFor(tags) =>
      val requestId = RequestId(Base16.encode(Random.nextBytes(32)))
      pendingRequests += requestId -> None
      broadcastTo(connections.getAll, GetFacts(requestId, tags, 10, currentTimeMillis()))
    case ListConnections =>
      sender() ! Connections(connections.getAll)
  }

  private def connectTo(peers: List[InetSocketAddress]): Unit =
    peers.foreach { remoteAddress =>
      self ! ConnectTo(remoteAddress)
    }

  private def broadcastTo(peers: List[InetSocketAddress], message: NetworkMessage): Unit =
    peers
      .foldLeft(List.empty[ActorRef]) {
        case (acc, peer) =>
          handlers.get(peer).fold(acc)(handlerRef => acc :+ handlerRef)
      }
      .foreach { handlerRef =>
        handlerRef ! message
      }

  private def handle(message: NetworkMessage, senderAddress: InetSocketAddress, senderRef: ActorRef): Unit =
    message match {
      case GetPeers(maxElems) =>
        // 1. Acquire as much peers from local storage as requested (excluding requester address)
        val peers =
          connections.getAll
            .filterNot(_ == senderAddress)
            .take(maxElems)
        if (peers.nonEmpty) {
          log.info(s"Sending [${peers.size}] known peers to [$senderAddress]")
          senderRef ! Peers(peers)
        }
      case Peers(addresses) =>
        val newPeers = addresses.filterNot(connections.contains)
        log.info(s"Got [${newPeers.size}] new peers from [$senderAddress]: [${newPeers.mkString(", ")}]")
        connectTo(newPeers)
      case req @ GetFacts(requestId, tags, ttl, _) =>
        factsServiceRef ! Get(tags, Some(requestId))
        pendingRequests += requestId -> Some(senderAddress)
        val ttlLeft = ttl - 1
        if (ttlLeft > 0) {
          // broadcast request to all connections except for the one this request came from.
          val peersToBroadcastTo = connections.getAll.filterNot(_ == senderAddress)
          broadcastTo(peersToBroadcastTo, req.copy(ttl = ttlLeft))
        }
      case res @ Facts(requestId, facts) =>
        pendingRequests.get(requestId) match {
          case Some(Some(requesterAddress)) =>
            handlers.get(requesterAddress).fold(log.warning(s"Handler for [$requesterAddress] not found")) {
              handlerRef =>
                // 1. send search result to the requester
                handlerRef ! res
                // 2. remove request from the registry
                pendingRequests -= requestId
            }
          case Some(None) => factsServiceRef ! Add(facts)
          case None =>
            log.warning(s"Got an undesired search result from [$senderAddress]")
        }
      case message =>
        log.warning(s"Got an unexpected message [$message] from [$senderAddress]")
    }
}

object NetworkController {

  final case class ConnectTo(remoteAddress: InetSocketAddress)
  final case class ConnectionLost(remoteAddress: InetSocketAddress)
  final case class Handshaked(remoteAddress: InetSocketAddress, ts: Long)
  final case class MessageFrom(remoteAddress: InetSocketAddress, message: NetworkMessage, ts: Long)
  final case class SearchFor(tags: List[Tag])
  case object ListConnections
  final case class Connections(peers: List[InetSocketAddress])

  def props(config: NetworkConfig, factsServiceRef: ActorRef): Props =
    Props(new NetworkController(config, factsServiceRef))
}
