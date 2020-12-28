package com.github.oskin1.factnet.services

import com.github.oskin1.factnet.domain.{FactId, Tag, TaggedFact}

trait FactsStore {

  def add(fact: TaggedFact): FactsStore

  def get(tags: List[Tag]): List[TaggedFact]

  def getAll: List[TaggedFact]
}

object FactsStore {

  def empty: FactsStore = new Impl(Map.empty, Map.empty)

  final class Impl(facts: Map[FactId, TaggedFact], index: Map[Tag, List[FactId]]) extends FactsStore {

    def add(fact: TaggedFact): FactsStore = {
      val factId       = fact.fact.id
      val updatedFacts = facts + (factId -> fact)
      val updatedIndex =
        fact.tags.foldLeft(index) {
          case (acc, tag) =>
            acc.get(tag).fold(acc + (tag -> List(factId)))(factIds => acc.updated(tag, factIds :+ factId))
        }
      new Impl(updatedFacts, updatedIndex)
    }

    def get(tags: List[Tag]): List[TaggedFact] =
      tags.foldLeft(List.empty[TaggedFact]) {
        case (acc, tag) =>
          index.get(tag).fold(acc)(factIds => acc ++ factIds.flatMap(facts.get))
      }

    def getAll: List[TaggedFact] =
      facts.values.toList
  }
}
