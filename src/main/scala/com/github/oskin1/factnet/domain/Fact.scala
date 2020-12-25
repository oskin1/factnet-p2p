package com.github.oskin1.factnet.domain

import scodec.Codec
import scodec.codecs.{uint16, utf8, variableSizeBits}

final case class Fact(value: String) extends AnyVal

object Fact {

  implicit val codec: Codec[Fact] =
    variableSizeBits(uint16, utf8).as[Fact]
}

final case class TaggedFact(fact: Fact, tags: List[String])
