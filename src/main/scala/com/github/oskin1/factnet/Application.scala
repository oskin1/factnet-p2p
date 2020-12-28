package com.github.oskin1.factnet

import akka.actor.ActorSystem
import com.github.oskin1.factnet.config.NetworkConfig
import com.github.oskin1.factnet.p2p.NetworkController
import pureconfig.ConfigSource

object Application extends App {

  implicit val system: ActorSystem = ActorSystem("factnet")

  def init(): Unit =
    ConfigSource.default.load[NetworkConfig] match {
      case Right(config) =>
        system.actorOf(NetworkController.props(config), "NetworkController")
      case Left(err) =>
        system.log.error(s"Failed to init app. $err")
    }

  init()
}
