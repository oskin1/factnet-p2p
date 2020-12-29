package com.github.oskin1.factnet.config

import java.net.InetSocketAddress

import pureconfig.ConfigReader

final case class HttpConfig(bindAddress: InetSocketAddress)

object HttpConfig {

  implicit val configReader: ConfigReader[HttpConfig] =
    pureconfig.generic.semiauto.deriveReader
}
