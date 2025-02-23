package dev.turtle.grenades

package utils

import Main.{coreprotectApi, plugin}
import enums.DropLocation
import utils.extras.{ExtraBlock, ExtraItemStack}
import utils.lang.Message.debugMessage

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.{Bukkit, Location, Material, World}

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}
import scala.math.{pow, sqrt}

object Blocks {
		case class ShrimpleBlock(block: Block, newMaterial: Material, dropItems: Integer, dropLocations: Array[DropLocation] = Array.empty[DropLocation], source: InventoryHolder = null)

		val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
		private var interval: Long = 1L
		private var maxBlocksPerLoop: Integer = 5000
		private var isQueuerRunning: Boolean = false

		private var blockQueue: mutable.Map[World, mutable.Queue[ShrimpleBlock]] = mutable.Map()

		def reloadVariables(newInterval: Long, newMaxBlocksPerLoop: Integer): Boolean = {
				this.isQueuerRunning = false
				this.interval = {
						if (newInterval < 1)
								1
						else newInterval
				}
				this.maxBlocksPerLoop = newMaxBlocksPerLoop
				runBlockPlaceExecutor()
				true
		}

		def runBlockPlaceExecutor(): Unit = {
				val initialDelay = 1L
				val task = new Runnable {
						def run(): Unit = {
								if (!isQueuerRunning) {
										debugMessage(s"&6block queue service &cstopped&6.")
										throw new RuntimeException("finished")
								}
								try
										placeBlocks()
								catch {
										case e: Throwable =>
												debugMessage(e.toString)
												debugMessage(e.getStackTrace.mkString("Array(", ", ", ")"))
								}
						}

						debugMessage(s"&6block queue service &2started&6. &7(&8${interval}&7mcs;&8${maxBlocksPerLoop}&7bpl)")
						isQueuerRunning = true
				}
				scheduler.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.MICROSECONDS)
		}

		def placeBlocks(): Unit = {
				for (world <- blockQueue.keys) {
						if (blockQueue.contains(world)) {
								0 to maxBlocksPerLoop foreach { _ =>
										if (blockQueue(world).nonEmpty) {
												val shrimpleBlock: ShrimpleBlock = blockQueue(world).dequeue()
												if (shrimpleBlock.newMaterial ne shrimpleBlock.block.getType)
														new BukkitRunnable() {
																@Override
																def run(): Unit = {
																		for (item <- shrimpleBlock.block.getAllDrops)
																				item.drop(shrimpleBlock.dropLocations, blockLoc = shrimpleBlock.block.getLocation, inventoryHolder = shrimpleBlock.source)
																		shrimpleBlock.block.setType(shrimpleBlock.newMaterial)
																}
														}.runTask(plugin)
										}
								}
						}
				}
		}

		def addToQueue(world: World, blocksArray: Array[ShrimpleBlock]): Boolean = {
				if (!isQueuerRunning)
						runBlockPlaceExecutor()
				if (blockQueue.contains(world)) {
						blockQueue(world) ++= blocksArray
				} else {
						blockQueue.put(world, mutable.Queue(blocksArray: _*))
				}
				debugMessage(s"&7Added ${blocksArray.length} blocks to queue", debugLevel = 10)
				true
		}

		def getInRadius(loc: Location, radius: Integer, player: Player = null): Future[Array[Block]] = Future /*Array[Block] =*/ {
				blocking {
						val blocks = for {
								x <- -radius to radius
								y <- -radius to radius
								z <- -radius to radius
								distFromCenter = Math.abs(x)
								distance = sqrt(pow(x, 2) + pow(y, 2) + pow(z, 2))
								if distance <= radius
						} yield loc.getWorld.getBlockAt(loc.getBlockX + x, loc.getBlockY + y, loc.getBlockZ + z)

						val filteredBlocks = player match {
								case null => blocks
								case _ => blocks.filter(canDestroyThatBlock(player, _))
						}

						filteredBlocks.toArray
				}
		}

		def canDestroyThatBlock(player: Player, block: Block): Boolean = {
				val breakEvent = new BlockBreakEvent(block, player)
				Bukkit.getServer.getPluginManager.callEvent(breakEvent)
				if (breakEvent.isCancelled) return false
				true
		}

		def setBlockType(b: Block, newType: Material, dropItems: Boolean, originName: String = "unknown"): Unit = {
				if (coreprotectApi.isDefined) {
						if (newType ne Material.AIR)
								coreprotectApi.get.logPlacement(originName, b.getLocation, newType, b.getBlockData)
						else {
								coreprotectApi.get.logRemoval(originName, b.getLocation, b.getType, b.getBlockData)
						}
				}
				if (dropItems && (newType eq Material.AIR)) b.breakNaturally
				b.setType(newType)
		}

}
