package com.github.oskin1.factnet.network

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.io.Tcp._
import akka.util.WallClock.AlwaysIncreasingClock.currentTimeMillis
import akka.util.{ByteString, CompactByteString}
import com.github.oskin1.factnet.network.NetworkController.{ConnectionLost, Handshaked, MessageFrom}
import com.github.oskin1.factnet.network.RemotePeerHandler.{Ack, CloseConnection, SendHandshake}
import scodec.Err.InsufficientBits
import scodec.{Attempt, DecodeResult}

import scala.annotation.tailrec
import scala.collection.immutable.TreeMap

final class RemotePeerHandler(
  connection: ActorRef,
  networkControllerRef: ActorRef,
  remoteAddress: InetSocketAddress
) extends Actor
  with ActorLogging {

  private var chunksBuffer: ByteString                          = CompactByteString.empty
  private var outgoingMessagesBuffer: TreeMap[Long, ByteString] = TreeMap.empty
  private var outgoingMessagesCounter: Long                     = 0

  override def preStart(): Unit = {
    context watch connection
    connection ! Register(self, keepOpenOnPeerClosed = false, useResumeWriting = true)
    connection ! ResumeReading
  }

  def receive: Receive = handshaking

  // Handshaking mode, other messages are ignore until then.
  private def handshaking: Receive = {
    case Received(data) =>
      NetworkMessage.decode(data) match {
        case Attempt.Successful(DecodeResult(_: Handshake, _)) =>
          log.info(s"Handshaked with $remoteAddress")
          networkControllerRef ! Handshaked(remoteAddress, currentTimeMillis())
          connection ! ResumeReading
          context become normal
        case _ =>
          log.warning(s"Failed to parse handshake message from $remoteAddress")
          self ! CloseConnection
      }
    case SendHandshake(localName) =>
      log.info(s"Handshaking with $remoteAddress")
      val rawMessage = NetworkMessage.encode(Handshake(1, currentTimeMillis(), localName))
      connection ! Write(rawMessage)
  }

  // Normal working mode
  private def normal: Receive =
    localWriting orElse
    reading orElse
    closeCommands

  private def buffering: Receive =
    localBuffering orElse
    reading orElse
    closeCommands

  private def closeCommands: Receive = {
    case CloseConnection =>
      log.info(s"Enforced to abort communication with $remoteAddress")
      pushAllWithNoAck()
      connection ! Abort // we're closing connection without waiting for a confirmation from the peer

    case cc: ConnectionClosed =>
      // connection closed from either side, actor is shutting down itself
      val reason: String =
        if (cc.isErrorClosed) "error: " + cc.getErrorCause
        else if (cc.isPeerClosed) "closed by the peer"
        else if (cc.isAborted) "aborted locally"
        else ""
      log.info(s"Connection closed to $remoteAddress, reason: " + reason)
      networkControllerRef ! ConnectionLost(remoteAddress)
      context stop self
  }

  def localWriting: Receive = {
    case message: NetworkMessage =>
      log.info(s"Sending $message to $remoteAddress")
      outgoingMessagesCounter += 1
      val rawMessage = NetworkMessage.encode(message)
      connection ! Write(rawMessage, Ack(outgoingMessagesCounter))

    case CommandFailed(Write(msg, Ack(id))) =>
      log.warning(s"Failed to write ${msg.length} bytes to $remoteAddress, switching to buffering mode")
      connection ! ResumeWriting
      buffer(id, msg)
      context become buffering

    case Ack(_) => // ignore ACKs in stable mode

    case WritingResumed => // ignore in stable mode
  }

  // operate in ACK mode until all buffered messages are transmitted
  def localBuffering: Receive = {
    case message: NetworkMessage =>
      outgoingMessagesCounter += 1
      val rawMessage = NetworkMessage.encode(message)
      buffer(outgoingMessagesCounter, rawMessage)

    case CommandFailed(Write(msg, Ack(id))) =>
      connection ! ResumeWriting
      buffer(id, msg)

    case CommandFailed(ResumeWriting) => // ignore in ACK mode

    case WritingResumed =>
      writeFirst()

    case Ack(id) =>
      outgoingMessagesBuffer -= id
      if (outgoingMessagesBuffer.nonEmpty) {
        writeFirst()
      } else {
        log.info("Buffered messages processed, exiting buffering mode")
        context become normal
      }
  }

  def reading: Receive = {
    case Received(data) =>
      chunksBuffer ++= data

      @tailrec
      def process(): Unit =
        NetworkMessage.decode(chunksBuffer) match {
          case Attempt.Successful(DecodeResult(message, _)) =>
            log.info(s"Received message $message from [$remoteAddress]")
            networkControllerRef ! MessageFrom(remoteAddress, message, currentTimeMillis())
            val messageLength = NetworkMessage.length(message)
            chunksBuffer = chunksBuffer.drop(messageLength)
            process()
          case Attempt.Failure(_: InsufficientBits) => // skip
          case Attempt.Failure(cause) =>
            log.info(s"Received invalid message from [$remoteAddress]. ${cause.message}")
        }

      process()
      connection ! ResumeReading
  }

  private def buffer(id: Long, msg: ByteString): Unit =
    outgoingMessagesBuffer += id -> msg

  private def writeFirst(): Unit =
    outgoingMessagesBuffer.headOption.foreach {
      case (id, msg) =>
        connection ! Write(msg, Ack(id))
    }

  // Write into the wire all the buffered messages we have for the peer with no ACK
  private def pushAllWithNoAck(): Unit =
    outgoingMessagesBuffer.foreach {
      case (_, msg) =>
        connection ! Write(msg)
    }
}

object RemotePeerHandler {
  final case class Ack(id: Long) extends Tcp.Event
  final case class SendHandshake(localName: String)
  case object CloseConnection

  def props(connection: ActorRef, networkControllerRef: ActorRef, remoteAddress: InetSocketAddress): Props =
    Props(new RemotePeerHandler(connection, networkControllerRef, remoteAddress))
}
