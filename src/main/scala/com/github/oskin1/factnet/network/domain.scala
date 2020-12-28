package com.github.oskin1.factnet.network

import java.net.InetSocketAddress

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs.{bytes, uint16, variableSizeBits}

object domain {

  final case class RequestId(value: String) extends AnyVal

  object RequestId {

    implicit val codec: Codec[RequestId] =
      variableSizeBits(uint16, bytes).xmap(
        x => RequestId(x.toBase16),
        s => ByteVector.fromValidHex(s.value)
      )
  }

  final case class RemoteId(value: String) extends AnyVal

  object RemoteId {

    def apply(inetSocketAddress: InetSocketAddress): RemoteId =
      new RemoteId(inetSocketAddress.toString)
  }
}
