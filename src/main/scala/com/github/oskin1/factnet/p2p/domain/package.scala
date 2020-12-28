package com.github.oskin1.factnet.p2p

import java.net.InetSocketAddress

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, uint16, variableSizeBits}

package object domain {

  final case class HexString(value: String) extends AnyVal

  object HexString {

    implicit val codec: Codec[HexString] =
      variableSizeBits(uint16, bytes).xmap(
        x => HexString(x.toBase16),
        s => ByteVector.fromValidHex(s.value)
      )
  }

  final case class RemoteId(value: String) extends AnyVal

  object RemoteId {
    def apply(inetSocketAddress: InetSocketAddress): RemoteId =
      new RemoteId(inetSocketAddress.toString)
  }
}
