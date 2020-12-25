package com.github.oskin1.factnet.p2p.domain

import java.net.{InetAddress, InetSocketAddress}

import scodec.Codec
import scodec.bits._
import scodec.codecs._

final case class RemoteAddress(address: ByteVector, port: Int) {

  override def toString: String = {
    val host =
      (address(0) & 0xff) + "." +
        (address(1) & 0xff) + "." +
        (address(2) & 0xff) + "." +
        (address(3) & 0xff)
    s"$host:$port"
  }
}

object RemoteAddress {

  private val IPv4R = "(\\d+.\\d+.\\d+.\\d+):(\\d+)".r

  def fromJava(inetAddress: InetSocketAddress): RemoteAddress =
    RemoteAddress(ByteVector(inetAddress.getAddress.getAddress), inetAddress.getPort)

  def fromIpv4StringUnsafe(s: String): RemoteAddress =
    fromIpv4String(s).toOption.get

  def fromIpv4String(s: String): Either[Exception, RemoteAddress] =
    s match {
      case IPv4R(host, port) =>
        val addr = ByteVector(InetAddress.getByName(host).getAddress)
        Right(RemoteAddress(addr, port.toInt))
      case _ =>
        Left(new Exception(s"$s is not a valid IPv4 address"))
    }

  implicit val codec: Codec[RemoteAddress] =
    (variableSizeBytes(uint8, bytes) :: uint16L).as[RemoteAddress]
}
