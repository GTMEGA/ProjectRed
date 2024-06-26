package mrtjp.projectred.transportation

import codechicken.lib.vec.BlockCoord
import codechicken.multipart.TileMultipart
import mrtjp.core.inventory.InvWrapper
import mrtjp.core.item.ItemKey
import net.minecraft.inventory.IInventory

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object PressurePriority {
  val backlog = 1 << 0
  val inventory = 1 << 1
}

object PressurePathFinder {
  var pipe: TPressureSubsystem = null
  var item: ItemKey = null
  var searchDirs = 0
  var colour = -1

  var invDirs = 0
  var backlogDirs = 0

  private var shortestDist = Integer.MAX_VALUE
  private var shortestBDist = Integer.MAX_VALUE

  def clear() {
    pipe = null
    item = null
    searchDirs = 0
    colour = -1
    invDirs = 0
    backlogDirs = 0
    shortestDist = Integer.MAX_VALUE
    shortestBDist = Integer.MAX_VALUE
  }

  def start() {
    val bc = new BlockCoord(pipe.tile)
    val q = Queue.newBuilder[Node]
    for (s <- 0 until 6 if (searchDirs & 1 << s) != 0 && pipe.maskConnects(s))
      q += Node(bc, s)
    iterate(q.result(), Set(Node(bc)))
  }

  @tailrec
  private def iterate(open: Seq[Node], closed: Set[Node] = Set.empty): Unit =
    open match {
      case Seq() =>
      case Seq(next, rest @ _*) =>
        getTile(next.bc) match {
          case dev: TPressureDevice =>
            if (dev.canAcceptInput(item, next.dir ^ 1)) setInvPath(next)
            else if (dev.canAcceptBacklog(item, next.dir ^ 1)) setBacklog(next)
            iterate(rest, closed + next)

          case inv: IInventory =>
            if (
              InvWrapper
                .wrap(inv)
                .setSlotsFromSide(next.dir ^ 1)
                .hasSpaceForItem(item)
            ) setInvPath(next)
            iterate(rest, closed + next)

          case tmp: TileMultipart =>
            tmp.partMap(6) match {
              case p: TPressureTube =>
                val upNext = Vector.newBuilder[Node]
                for (s <- 0 until 6)
                  if (s != (next.dir ^ 1) && p.maskConnects(s)) {
                    val route = next --> (s, p.getPathWeight, p.pathFilter(
                      next.dir ^ 1,
                      s
                    ))
                    if (
                      route.flagRouteTo && route.allowColor(colour) && route
                        .allowItem(item)
                    )
                      if (!closed(route) && !open.contains(route))
                        upNext += route
                  }
                iterate(rest ++ upNext.result(), closed + next)

              case _ => iterate(rest, closed + next)
            }
          case _ => iterate(rest, closed + next)
        }
      case _ =>
    }

  private def setInvPath(n: Node) {
    if (n.dist < shortestDist) {
      shortestDist = n.dist
      invDirs = 0
    }
    if (n.dist == shortestDist) invDirs |= 1 << n.hop
  }

  private def setBacklog(n: Node) {
    if (n.dist < shortestBDist) {
      shortestBDist = n.dist
      backlogDirs = 0
    }
    if (n.dist == shortestBDist) backlogDirs |= 1 << n.hop
  }

  private def getTile(bc: BlockCoord) =
    pipe.world.getTileEntity(bc.x, bc.y, bc.z)

  private object Node {
    def apply(bc: BlockCoord): Node = new Node(bc.copy, 0, 6, 6)
    def apply(bc: BlockCoord, dir: Int): Node =
      new Node(bc.copy.offset(dir), 1, dir, dir)
  }
  private class Node(
      val bc: BlockCoord,
      val dist: Int,
      val dir: Int,
      val hop: Int,
      filters: Set[PathFilter] = Set.empty
  ) extends Path(filters)
      with Ordered[Node] {
    def -->(toDir: Int, distAway: Int, filter: PathFilter): Node = new Node(
      bc.copy.offset(toDir),
      dist + distAway,
      toDir,
      hop,
      filters + filter
    )
    def -->(toDir: Int, distAway: Int): Node =
      new Node(bc.copy.offset(toDir), dist + distAway, toDir, hop, filters)
    def -->(toDir: Int): Node = this --> (toDir, 1)

    override def compare(that: Node) = dist - that.dist

    override def equals(other: Any) = other match {
      case that: Node =>
        bc == that.bc && dir == that.dir
      case _ => false
    }

    override def hashCode = bc.hashCode

    override def toString =
      "@" + bc.toString + ": delta(" + dir + ") hop(" + hop + ")"
  }
}
