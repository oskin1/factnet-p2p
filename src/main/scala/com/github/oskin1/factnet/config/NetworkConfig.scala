package com.github.oskin1.factnet.config

import java.net.InetSocketAddress

import pureconfig.ConfigReader

import scala.concurrent.duration.FiniteDuration

final case class NetworkConfig(
  localName: String,
  bindAddress: InetSocketAddress,
  knownPeers: List[InetSocketAddress],
  minConnections: Int,
  maxConnections: Int,
  connectionTimeout: FiniteDuration
)

object NetworkConfig {

  implicit val configReader: ConfigReader[NetworkConfig] =
    pureconfig.generic.semiauto.deriveReader
}
