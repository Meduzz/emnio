package se.chimps.emnio

import se.chimps.emnio.server.{Builder, Config}

object Server {
	def builder():Builder = Config()
}
