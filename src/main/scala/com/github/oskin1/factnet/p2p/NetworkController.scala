package com.github.oskin1.factnet.p2p

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.WallClock.AlwaysIncreasingClock.currentTimeMillis
import com.github.oskin1.factnet.config.NetworkConfig
import com.github.oskin1.factnet.p2p.NetworkController.{ConnectTo, Handshaked, Seen}
import com.github.oskin1.factnet.p2p.RemotePeerHandler.SendHandshake

final class NetworkController(config: NetworkConfig) extends Actor with ActorLogging {

  import context.system

  private val tcpManager = IO(Tcp)

  private var connections = PeerStore.empty(config.maxConnections)

  override def preStart(): Unit =
    tcpManager ! Bind(self, config.bindAddress)

  def receive: Receive = binding

  private def binding: Receive = {
    case Bound(localAddress) =>
      log.info(s"Bound to [$localAddress]")
      bootstrap()
      context become working
    case CommandFailed(_: Bind) =>
      log.error(s"Failed to bind to [${config.bindAddress}]")
      context stop self
      sys.exit(1)
  }

  private def working: Receive =
    commands orElse remoteConnections orElse logUnhandled

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
      connections = connections.remove(remoteAddress)
    case Seen(remoteAddress, seenAt) =>
      connections = connections.seen(remoteAddress, seenAt)
    case Handshaked(remoteAddress, ts) =>
      log.info(s"A connection to [$remoteAddress] confirmed")
      connections = connections.confirm(remoteAddress, ts)
  }

  private def bootstrap(): Unit =
    config.knownPeers.foreach { remoteAddress =>
      self ! ConnectTo(remoteAddress)
    }
}

object NetworkController {

  final case class ConnectTo(remoteAddress: InetSocketAddress)
  final case class Seen(remoteAddress: InetSocketAddress, ts: Long)
  final case class Handshaked(remoteAddress: InetSocketAddress, ts: Long)

  def props(config: NetworkConfig): Props =
    Props(new NetworkController(config))
}
