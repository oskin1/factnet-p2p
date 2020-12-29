package com.github.oskin1.factnet.network

import java.net.InetSocketAddress

import akka.util.ByteString
import cats.Show
import com.github.oskin1.factnet.domain.{Tag, TaggedFact}
import com.github.oskin1.factnet.network.codecs._
import com.github.oskin1.factnet.network.domain.RequestId
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult}
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

  def length(message: NetworkMessage): Int =
    codec.encode(message).require.toByteArray.length

  def decode(raw: ByteString): Attempt[DecodeResult[NetworkMessage]] =
    codec.decode(BitVector(raw.asByteBuffer))

  def encode(message: NetworkMessage): ByteString =
    ByteString(codec.encode(message).require.toByteArray)
}

final case class Handshake(
  version: Int,
  timestamp: Long,
  peerName: String
) extends NetworkMessage

object Handshake {
  val MessageCode: Byte = 0

  implicit val codec: Codec[Handshake] = ??? // todo
}

final case class GetPeers(maxElems: Int) extends NetworkMessage

object GetPeers {
  val MessageCode: Byte = 1

  implicit val codec: Codec[GetPeers] = ??? // todo
}

final case class Peers(addresses: List[InetSocketAddress]) extends NetworkMessage

object Peers {
  val MessageCode: Byte = 2

  implicit val codec: Codec[Peers] = ??? // todo
}

final case class GetFacts(requestId: RequestId, tags: List[Tag], ttl: Int, timestamp: Long)
  extends NetworkMessage

object GetFacts {
  val MessageCode: Byte = 3

  implicit val codec: Codec[GetFacts] = ??? // todo
}

final case class Facts(requestId: RequestId, facts: List[TaggedFact]) extends NetworkMessage

object Facts {
  val MessageCode: Byte = 4

  implicit val codec: Codec[Facts] = ??? // todo
}
