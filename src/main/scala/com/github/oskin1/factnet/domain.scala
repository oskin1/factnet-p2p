package com.github.oskin1.factnet

import io.circe.{Decoder, Encoder}
import scodec.Codec
import scodec.codecs.{listOfN, uint16, utf8, variableSizeBits}
import scorex.util.encode.Base16
import tsec.hashing.jca.SHA256

object domain {

  final case class Tag(value: String) extends AnyVal

  object Tag {

    implicit val encoder: Encoder[Tag] = implicitly[Encoder[String]].contramap(_.value)
    implicit val decoder: Decoder[Tag] = implicitly[Decoder[String]].map(Tag.apply)

    implicit val codec: Codec[Tag] =
      variableSizeBits(uint16, utf8).as[Tag]
  }

  final case class FactId(value: String) extends AnyVal

  final case class Fact(value: String) extends AnyVal {
    def id: FactId = FactId(Base16.encode(SHA256.unsafeHash(value.getBytes)))
  }

  object Fact {

    implicit val encoder: Encoder[Fact] = implicitly[Encoder[String]].contramap(_.value)
    implicit val decoder: Decoder[Fact] = implicitly[Decoder[String]].map(Fact.apply)

    implicit val codec: Codec[Fact] =
      variableSizeBits(uint16, utf8).as[Fact]
  }

  final case class TaggedFact(fact: Fact, tags: List[Tag])

  object TaggedFact {

    implicit val jsonCodec: io.circe.Codec[TaggedFact] = io.circe.generic.semiauto.deriveCodec

    implicit val codec: Codec[TaggedFact] =
      (implicitly[Codec[Fact]] :: listOfN(uint16, implicitly[Codec[Tag]])).as[TaggedFact]
  }
}
