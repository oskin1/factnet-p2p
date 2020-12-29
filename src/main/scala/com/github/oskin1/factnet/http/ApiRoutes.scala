package com.github.oskin1.factnet.http

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.github.oskin1.factnet.domain.{Tag, TaggedFact}
import com.github.oskin1.factnet.network.NetworkController.{Connections, ListConnections, SearchFor}
import com.github.oskin1.factnet.services.FactsService.{Add, Get, GetAll, SearchResult}
import io.circe.{Encoder, Json}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

final class ApiRoutes(networkControllerRef: ActorRef, factsServiceRef: ActorRef)(implicit ec: ExecutionContext) {

  implicit private val requestTimeout: Timeout = 10.seconds

  val routes: Route = getConnectionsR ~ pathPrefix("facts")(getAllFactsR ~ getFactsR ~ searchR ~ addFactsR)

  private def getAllFactsR: Route =
    (path("get" / "all") & get) {
      complete((factsServiceRef ? GetAll).mapTo[SearchResult].map(_.facts))
    }

  private def getFactsR: Route =
    (path("get" / Segment) & get) { tag =>
      complete((factsServiceRef ? Get(List(Tag(tag)), None)).mapTo[SearchResult].map(_.facts))
    }

  private def addFactsR: Route =
    (path("add") & post & entity(as[List[TaggedFact]])) { facts =>
      factsServiceRef ! Add(facts)
      Directives.complete(StatusCodes.OK)
    }

  private def searchR: Route =
    (path("search" / Segment) & post) { tag =>
      networkControllerRef ! SearchFor(List(Tag(tag)))
      Directives.complete(StatusCodes.OK)
    }

  private def getConnectionsR: Route =
    (path("connections") & get) {
      complete((networkControllerRef ? ListConnections).mapTo[Connections].map(_.peers))
    }

  private def complete[R](result: Future[R])(implicit encoder: Encoder[R]): Route =
    Directives.onSuccess(result)(res => complete(encoder(res)))

  private def complete(result: Json): Route =
    if (result.isNull) {
      Directives.complete(StatusCodes.NotFound)
    } else {
      val httpEntity = HttpEntity(ContentTypes.`application/json`, result.spaces2)
      Directives.complete(StatusCodes.OK.intValue -> httpEntity)
    }
}
