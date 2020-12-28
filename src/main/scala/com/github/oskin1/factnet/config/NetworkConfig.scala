package com.github.oskin1.factnet.config

import java.net.{InetAddress, InetSocketAddress}

import cats.syntax.either._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

final case class NetworkConfig(
  localName: String,
  bindAddress: InetSocketAddress,
  knownPeers: List[InetSocketAddress],
  minConnections: Int,
  maxConnections: Int,
  connectionTimeout: FiniteDuration
)

object NetworkConfig {

  implicit val inetSocketAddressConfigReader: ConfigReader[InetSocketAddress] =
    ConfigReader[String].emap { s =>
      val Array(host, port) = s.split(':')
      Try(new InetSocketAddress(InetAddress.getByName(host), port.toInt)).toEither
        .leftMap(t => CannotConvert(s, "InetSocketAddress", t.getMessage))
    }

  implicit val configReader: ConfigReader[NetworkConfig] =
    pureconfig.generic.semiauto.deriveReader
}
