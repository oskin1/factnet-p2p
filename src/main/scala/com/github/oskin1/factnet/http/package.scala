package com.github.oskin1.factnet

import java.net.InetSocketAddress

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

package object http {

  implicit val inetSocketAddressEncoder: Encoder[InetSocketAddress] = _.toString.asJson

  implicit def um[A](implicit decoder: Decoder[A], ec: ExecutionContext): FromRequestUnmarshaller[A] =
    Unmarshaller.withMaterializer[HttpRequest, A] { _ => mat => req =>
      req.entity.toStrict(5.seconds)(mat).flatMap { bs =>
        val str = new String(bs.data.toArray)
        io.circe.parser.decode(str).fold(Future.failed, Future.successful)
      }
    }
}
