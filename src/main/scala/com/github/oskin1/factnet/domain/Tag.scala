package com.github.oskin1.factnet.domain

import scodec.Codec
import scodec.codecs._

final case class Tag(value: String) extends AnyVal

object Tag {

  implicit val codec: Codec[Tag] =
    variableSizeBits(uint16, utf8).as[Tag]
}
