import sbt.*

object Dependencies {
		val onelibVersion = "1.0.1b"

		lazy val commonDeps: Seq[ModuleID] = Seq(
				"org.spigotmc" % "spigot-api" % "1.21.4-R0.1-SNAPSHOT" % "provided",
				"de.tr7zw" % "item-nbt-api" % "2.12.0",
				"net.coreprotect" % "coreprotect" % "21.3" % "provided",
				"com.typesafe" % "config" % "1.4.3",
				"com.github.TurtleGithubQuest" % "OneLib" % onelibVersion % "provided"
		)
}