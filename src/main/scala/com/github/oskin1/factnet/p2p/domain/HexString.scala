package com.github.oskin1.factnet.p2p.domain

import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

final case class HexString(value: String) extends AnyVal

object HexString {

  implicit val codec: Codec[HexString] =
    variableSizeBits(uint16, bytes).xmap(
      x => HexString(x.toBase16),
      s => ByteVector.fromValidHex(s.value)
    )
}
