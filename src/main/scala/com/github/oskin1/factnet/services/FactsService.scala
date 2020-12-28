package com.github.oskin1.factnet.services

import akka.actor.{Actor, Props}
import com.github.oskin1.factnet.domain.{Tag, TaggedFact}
import com.github.oskin1.factnet.network.domain.RequestId
import com.github.oskin1.factnet.services.FactsService.{Add, Get, GetAll, SearchResult}

class FactsService extends Actor {

  private var store = FactsStore.empty

  def receive: Receive = {
    case Add(facts) =>
      facts.foreach(fact => store = store.add(fact))
    case Get(tags, requestId) =>
      val result = store.get(tags)
      sender() ! SearchResult(result, requestId)
    case GetAll =>
      val result = store.getAll
      sender() ! SearchResult(result, None)
  }
}

object FactsService {

  final case class Add(facts: List[TaggedFact])
  final case class SearchResult(facts: List[TaggedFact], requesterId: Option[RequestId])
  final case class Get(tags: List[Tag], requesterId: Option[RequestId])
  case object GetAll

  def props: Props = Props(new FactsService)
}
