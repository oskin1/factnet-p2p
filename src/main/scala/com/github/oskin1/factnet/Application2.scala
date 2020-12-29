package com.github.oskin1.factnet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.github.oskin1.factnet.config.ConfigBundle
import com.github.oskin1.factnet.http.ApiRoutes
import com.github.oskin1.factnet.network.NetworkController
import com.github.oskin1.factnet.services.FactsService
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext

object Application2 extends App {

  implicit val system: ActorSystem  = ActorSystem("factnet")
  implicit val ec: ExecutionContext = system.dispatcher

  def init(): Unit =
    ConfigSource.default.load[ConfigBundle] match {
      case Right(config) =>
        val factsService = system.actorOf(FactsService.props)
        val networkController =
          system.actorOf(NetworkController.props(config.network, factsService), "NetworkController")
        val httpAddress = config.http.bindAddress
        val httpRoutes  = new ApiRoutes(networkController, factsService)
        Http().newServerAt(httpAddress.getHostString, httpAddress.getPort).bindFlow(httpRoutes.routes)
      case Left(err) =>
        system.log.error(s"Failed to init app. $err")
    }

  init()
}
