package com.github.oskin1.factnet.p2p

import java.net.InetSocketAddress

trait PeerStore {

  def size: Int

  def add(address: InetSocketAddress, ts: Long): Either[String, PeerStore]

  def seen(address: InetSocketAddress, ts: Long): PeerStore

  def confirm(address: InetSocketAddress, ts: Long): PeerStore

  def remove(address: InetSocketAddress): PeerStore

  def contains(address: InetSocketAddress): Boolean

  def getAll: List[InetSocketAddress]
}

object PeerStore {

  final case class PeerInfo(lastSeen: Long, confirmed: Boolean)

  def empty(poolLimit: Int): PeerStore = new Impl(Map.empty, poolLimit)

  final class Impl(pool: Map[InetSocketAddress, PeerInfo], sizeBound: Int) extends PeerStore {

    def size: Int = pool.size

    def add(address: InetSocketAddress, ts: Long): Either[String, PeerStore] =
      if (pool.size < sizeBound)
        Right(new Impl(pool + (address -> PeerInfo(ts, confirmed = false)), sizeBound))
      else Left("Pool limit exceeded")

    def seen(address: InetSocketAddress, seenAt: Long): PeerStore = {
      val updatedPool =
        pool.get(address).fold(pool)(peerInfo => pool.updated(address, peerInfo.copy(lastSeen = seenAt)))
      new Impl(updatedPool, sizeBound)
    }

    def confirm(address: InetSocketAddress, ts: Long): PeerStore = {
      val updatedPool = pool.updated(address, PeerInfo(ts, confirmed = true))
      new Impl(updatedPool, sizeBound)
    }

    def remove(address: InetSocketAddress): PeerStore =
      new Impl(pool - address, sizeBound)

    def contains(address: InetSocketAddress): Boolean = pool.contains(address)

    def getAll: List[InetSocketAddress] = pool.keys.toList
  }
}
