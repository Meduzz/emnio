package se.chimps.emnio.example

import monix.eval.Task
import monix.execution.Scheduler
import se.chimps.emnio.Client

object EchoClient {

	def main(args:Array[String]):Unit = {
		val client = Client()
		val scheduler = Scheduler.global

		client.connect("127.0.0.1", 2233)
  		.foreach(c => {
			  println("writing data")
			  c.write("Hello world!".getBytes("utf-8"))
			  c.onData()
  			  .doOnError(e => Task(println(e.getMessage)))
  			  .foreach(d => {
				    println(s"Server echoed: ${new String(d, "utf-8")}")
				    c.close()
				    System.exit(0)
			    })(scheduler)
		  })(scheduler)
	}
}
