package com.github.oskin1.factnet

import java.net.{InetAddress, InetSocketAddress}

import cats.syntax.either._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

import scala.util.Try

package object config {

  implicit val inetSocketAddressConfigReader: ConfigReader[InetSocketAddress] =
    ConfigReader[String].emap { s =>
      val Array(host, port) = s.split(':')
      Try(new InetSocketAddress(InetAddress.getByName(host), port.toInt)).toEither
        .leftMap(t => CannotConvert(s, "InetSocketAddress", t.getMessage))
    }
}
