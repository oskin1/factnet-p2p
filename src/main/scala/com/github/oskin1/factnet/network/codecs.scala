package com.github.oskin1.factnet.network

import java.net.{InetAddress, InetSocketAddress}

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

object codecs {

  implicit val inetSocketAddressCodec: Codec[InetSocketAddress] =
    (variableSizeBytes(uint8, bytes) ~ int32).xmapc[InetSocketAddress] {
      case (address, port) => new InetSocketAddress(InetAddress.getByAddress(address.toArray), port)
    } {
      inetSocketAddress => (ByteVector(inetSocketAddress.getAddress.getAddress), inetSocketAddress.getPort)
    }
}
