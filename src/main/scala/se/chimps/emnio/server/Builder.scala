package se.chimps.emnio.server

trait Builder {
	def withHostname(hostOrIp:String):Builder
	def withPort(port:Int):Builder
	def build():Server
}

case class Config(hostOrIp:String = "127.0.0.1", port:Int = 0) extends Builder {

	override def withHostname(hostOrIp:String):Builder = copy(hostOrIp = hostOrIp)

	override def withPort(port:Int):Builder = copy(port = port)

	override def build():Server = new Server(this)
}