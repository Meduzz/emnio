package se.chimps.emnio.client

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

import monix.reactive.{Observable}
import se.chimps.emnio.common.{Connection, DataLoop}

import scala.util.{Failure, Success, Try}

class Client {
	private val dataloop = new DataLoop()
	private val loop:Thread = new Thread(dataloop)

	def connect(address:String, port:Int):Observable[Connection] = {
		Try {
			val socket = SocketChannel.open(new InetSocketAddress(address, port))
			val conn = Connection(socket, dataloop)

			socket.configureBlocking(false)

			dataloop.registerConnection(conn)

			conn
		} match {
			case Success(conn) => Observable.now(conn)
			case Failure(e) => // TODO here the monix thingie fell short and could not provide me with a way to send the exception. Observable.
				/*
					And Observable.fromTry seems to simply swallow the exception and stop executing.

					I see 2 options.
					1. Roll our own observable impl.
					2. Roll our own reactive streams impl.
				 */
		}
	}

	loop.start()
}
