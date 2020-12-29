package com.github.oskin1.factnet.config

import pureconfig.ConfigReader

final case class ConfigBundle(http: HttpConfig, network: NetworkConfig)

object ConfigBundle {

  implicit val configReader: ConfigReader[ConfigBundle] =
    pureconfig.generic.semiauto.deriveReader
}
