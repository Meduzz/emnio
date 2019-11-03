package se.chimps.emnio.server

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import se.chimps.emnio.common.{Connection, DataLoop}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class Server(private val config:Config) {
	private var socket:ServerSocketChannel = _
	private var running = true
	private var loop:Thread = _

	private val dataloop = new DataLoop()
	private val subject = PublishSubject[Connection]()

	def onConnect():Observable[Connection] = subject.subscribeOn(Scheduler.global)

	def start():Unit = {
		socket = ServerSocketChannel.open()
		socket.bind(new InetSocketAddress(config.port))

		loop = new Thread(dataloop)

		loop.start()

		println(s"Emnio started at port: ${socket.getLocalAddress}")
		connectionLoop()
	}

	def stop():Unit = {
		running = false
		dataloop.stop()
	}

	@tailrec
	private def connectionLoop():Unit = {
		Try(socket.accept()) match {
			case Success(value) => {
				value.configureBlocking(false)

				val conn = Connection(value, dataloop)
				dataloop.registerConnection(conn)
				subject.onNext(conn)
			}
			case Failure(e) => subject.onError(e)
		}

		if (running) {
			connectionLoop()
		}
	}
}
