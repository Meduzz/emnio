package se.chimps.emnio.example

import monix.execution.Scheduler
import se.chimps.emnio.Server

import scala.sys.ShutdownHookThread

object EchoServer {
	def main(args:Array[String]):Unit = {
		val server = Server.builder()
  		.withPort(2233)
  		.build()

		val scheduler = Scheduler.global

		server.onConnect()
  		.foreach(c => {
			  c.onData()
  			  .foreach(b => {
				    c.write(b)
			    })(scheduler)
		  })(scheduler)

		ShutdownHookThread {
			server.stop()
		}

		server.start()
	}
}
