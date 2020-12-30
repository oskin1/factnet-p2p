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
    // 1. Bootstrap
    // todo
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
    // Connect to the desired address unless the connection to it already exists
    // todo
  }

  private def remoteConnections: Receive = {
    case Connected(remoteAddress, localAddress) =>
      log.info(s"Successfully connected to [$localAddress -> $remoteAddress]")
    // 1. try adding new connection to the peers pool
    // 2. update peers pool
    // 3. spawn a new connection handler
    // 4. send handshake to the peer
    // todo
    case cf @ CommandFailed(Connect(remoteAddress, _, _, _, _)) =>
      log.warning(s"Failed to connect to [$remoteAddress]. ${cf.cause.map(_.getMessage).getOrElse("?")}")
    // 1. drop failed connection
    case ConnectionLost(remoteAddress) =>
    // 1. remove corresponding handler from the registry
    // 2. drop lost connection
    // todo
    case MessageFrom(remoteAddress, message, seenAt) =>
    // 1. update timestamp `remoteAddress` was last seen
    // 2. handle concrete message
    // todo
    case Handshaked(remoteAddress, ts) =>
      log.info(s"A connection to [$remoteAddress] confirmed")
    // 1. confirm connection to `remoteAddress`
    // 2. register a handler for a confirmed connection
    // 3. discover more peers if needed
    // todo
  }

  private def search: Receive = {
    case SearchResult(facts, Some(requestId)) =>
    // Handle result of local lookup
    // 1. Find a corresponding requester address in the registry
    // 2. Find a corresponding handler
    // 3. Send result to the remote peer
    // todo
    case SearchFor(tags) =>
    // Handle local search request
    // 1. Generate request ID
    // 2. Register local search request
    // 3. Broadcast search request to all known peers
    // todo
    case ListConnections =>
      sender() ! Connections(connections.getAll)
  }

  /** Connect to given `peers`.
    */
  private def connectTo(peers: List[InetSocketAddress]): Unit = ??? // todo

  /** Broadcast a given `message` to a given `peers`.
    */
  private def broadcastTo(peers: List[InetSocketAddress], message: NetworkMessage): Unit = ??? // todo

  /** Handle specific network messages.
    */
  private def handle(message: NetworkMessage, senderAddress: InetSocketAddress, senderRef: ActorRef): Unit =
    message match {
      case GetPeers(maxElems) =>
      // Handle peers request
      // 1. Acquire as much peers from local storage as requested (excluding requester address)
      // 2. Send peers to the requester
      // todo
      case Peers(addresses) =>
      // Handle peers response
      // 1. Select peers we haven't seen before
      // 2. Connect to new peers
      // todo
      case req @ GetFacts(requestId, tags, ttl, _) =>
      // Handle search request
      // 1. Perform local lookup
      // 2. Register request in order to pass results back to the correct requester
      // 3. Decrement request TTL
      // 4. Broadcast request to all connections except for the one this request came from (unless request is expired).
      // todo
      case res @ Facts(requestId, facts) =>
      // Handle search result
      // 1. Try to acquire requester address form the registry
      // 2a. In case requester address is present find the corresponding handler
      // 3a. Send search result to the requester
      // 2b. In case requester address is None save the result to local store
      // todo
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
