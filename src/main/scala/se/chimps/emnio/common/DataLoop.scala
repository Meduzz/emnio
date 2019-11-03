package se.chimps.emnio.common

import java.nio.channels.{SelectionKey, Selector}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class DataLoop extends Runnable {
	private val readSelector = Selector.open()
	private val writeSelector = Selector.open()

	private var running = true
	private var connections:Seq[Connection] = Seq()

	override def run():Unit = {
		dataLoop()
	}

	@tailrec
	private def dataLoop():Unit = {
		// Reads
		Try(readSelector.select(100)) match  {
			case Success(count) => {
				if (count > 0) {
					val javaKeys = readSelector.selectedKeys()
					val keys = javaKeys.iterator().asScala

					javaKeys.asScala.map(key => {
						handleRead(key)
						key
					}).foreach(key => javaKeys.remove(key))
				}
			}
			case Failure(e) => {
				println("Read selector threw error in dataloop")
				e.printStackTrace()
			}
		}

		// Writes
		Try(writeSelector.select(100)) match {
			case Success(count) => {
				val javaKeys = writeSelector.selectedKeys()
				val keys = javaKeys.iterator().asScala

				for (key <- keys) {
					handleWrite(key)
					javaKeys.remove(key)
				}
			}
			case Failure(e) => {
				println("Write selector threw error in dataloop")
				e.printStackTrace()
			}
		}

		if (running) {
			dataLoop()
		}
	}

	private def handleRead(key:SelectionKey):Unit = {
		val conn = key.attachment().asInstanceOf[Connection]

		if (key.isReadable) {
			conn.doRead()
		}
	}

	private def handleWrite(key:SelectionKey):Unit = {
		val conn = key.attachment().asInstanceOf[Connection]

		if (key.isWritable) {
			conn.doWrite()
			key.cancel()
		}
	}

	def registerConnection(conn:Connection):Unit = {
		conn.withChannel(s => {
			if (s.isOpen && s.isConnected) {
				val key = s.register(readSelector, SelectionKey.OP_READ)
				key.attach(conn)
			}
		})

		connections = connections ++ Seq(conn)
	}

	def signalWrite(conn:Connection):Unit = {
		conn.withChannel(s => {
			if (s.isOpen && s.isConnected) {
				val key = s.register(writeSelector, SelectionKey.OP_WRITE)
				key.attach(conn)
			}
		})
	}

	def stop():Unit = {
		running = false
	}
}
