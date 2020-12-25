package com.github.oskin1.factnet.p2p

import cats.Show
import com.github.oskin1.factnet.p2p.domain.RemoteAddress
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._
import tsec.hashing.jca.SHA256

sealed abstract class NetworkMessage

object NetworkMessage {

  implicit val show: Show[NetworkMessage] = _.toString

  private val mkChecksum: BitVector => BitVector = xs =>
    ByteVector(SHA256.unsafeHash(SHA256.unsafeHash(xs.toByteArray)).take(4)).toBitVector

  private def checkedCodec[A: Codec] =
    checksummed(implicitly[Codec[A]], mkChecksum, variableSizeBytes(int32, bits) ~ bits(32))

  implicit val codec: Codec[NetworkMessage] =
    discriminated[NetworkMessage]
      .by(byte)
      .caseP(Handshake.MessageCode) { case h: Handshake => h }(identity)(checkedCodec[Handshake])
      .caseP(GetPeers.MessageCode) { case h: GetPeers => h }(identity)(checkedCodec[GetPeers])
      .caseP(Peers.MessageCode) { case h: Peers => h }(identity)(checkedCodec[Peers])
      .caseP(GetFacts.MessageCode) { case h: GetFacts => h }(identity)(checkedCodec[GetFacts])
      .caseP(Facts.MessageCode) { case h: Facts => h }(identity)(checkedCodec[Facts])
}

final case class Handshake(
  version: Int,
  timestamp: Long
) extends NetworkMessage

object Handshake {
  val MessageCode: Byte = 0

  implicit val codec: Codec[Handshake] =
    (int32 :: int64).as[Handshake]
}

final case class GetPeers(maxElems: Int) extends NetworkMessage

object GetPeers {
  val MessageCode: Byte = 1

  implicit val codec: Codec[GetPeers] =
    int32.as[GetPeers]
}

final case class Peers(addresses: List[RemoteAddress]) extends NetworkMessage

object Peers {
  val MessageCode: Byte = 2

  implicit val codec: Codec[Peers] =
    listOfN(uint16, implicitly[Codec[RemoteAddress]]).as[Peers]
}

final case class GetFacts(requester: RemoteAddress, tags: List[Tag], ttl: Int, timestamp: Long) extends NetworkMessage

object GetFacts {
  val MessageCode: Byte = 3

  implicit val codec: Codec[GetFacts] =
    (implicitly[Codec[RemoteAddress]] :: listOfN(uint16, implicitly[Codec[Tag]]) :: int32 :: int64).as[GetFacts]
}

final case class Facts(requestId: HexString, facts: List[Fact]) extends NetworkMessage

object Facts {
  val MessageCode: Byte = 4

  implicit val codec: Codec[Facts] =
    (implicitly[Codec[HexString]] :: listOfN(uint16, implicitly[Codec[Fact]])).as[Facts]
}
