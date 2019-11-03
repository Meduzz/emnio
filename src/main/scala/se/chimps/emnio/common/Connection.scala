package se.chimps.emnio.common

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

trait Connection {
	def onData():Observable[Array[Byte]]

	def write(data:Array[Byte]):Unit

	def withChannel(delegate:SocketChannel => Unit):Unit

	def close():Unit

	private[emnio] def doRead():Unit

	private[emnio] def doWrite():Unit
}

object Connection {
	def apply(connection:SocketChannel, loop:DataLoop):Connection = new ConnWrapper(connection, loop)
}

class ConnWrapper(conn:SocketChannel, loop:DataLoop) extends Connection {

	private val publisher = PublishSubject[Array[Byte]]()
	private var buf = Seq[Array[Byte]]()

	override def onData():Observable[Array[Byte]] = publisher.subscribeOn(Scheduler.global)

	override def write(data:Array[Byte]):Unit = {
		buf = buf ++ Seq(data)
		loop.signalWrite(this)
	}

	override def withChannel(delegate:SocketChannel => Unit):Unit = delegate(conn)

	override def close():Unit = conn.close()

	override private[emnio] def doRead():Unit = {
		val bytes = read()
  		.fold(Array[Byte]())((a, ne) => a ++ ne)

		publisher.onNext(bytes)
	}

	override private[emnio] def doWrite():Unit = {
		if (buf.nonEmpty) {
			val written = write(buf)

			buf = Seq()
		}
	}

	private def read():Seq[Array[Byte]] = {
		val buffer = ByteBuffer.allocate(1024)
		val bytesRead = conn.read(buffer)

		if (bytesRead > 0) {
			val data = Array.ofDim[Byte](buffer.position())
			buffer.flip()
			buffer.get(data)
			Seq(data) ++ read()
		} else {
			Seq()
		}
	}

	private def write(buffer:Seq[Array[Byte]]):Long = {
		val buf = ByteBuffer.wrap(buffer.head)
		var len = 0

		while (buf.remaining() > 0) {
			len = len + conn.write(buf)
		}

		if (buffer.tail.nonEmpty) {
			len + write(buffer.tail)
		} else {
			len
		}
	}
}