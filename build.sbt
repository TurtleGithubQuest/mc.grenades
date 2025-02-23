ThisBuild / version := "1.9"
ThisBuild / scalaVersion := "3.3.1"

ThisBuild / resolvers ++= Seq(
		"Jitpack" at "https://jitpack.io",
		"Spigot Snapshots" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",
		"codemc-repo" at "https://repo.codemc.org/repository/maven-public/",
		"net-coreprotect" at "https://maven.playpro.com"
)

ThisBuild / libraryDependencies ++= Dependencies.commonDeps

inThisBuild(DockerTasks.dockerSettings)

lazy val core = project.in(file("."))
	.settings(Common.commonSettings *)
	.settings(
			libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % Dependencies.onelibVersion % "provided")
	)

lazy val slimmy = project
	.in(file("slim_files"))
	.dependsOn(core)
	.settings((Common.commonSettings ++ Common.slimSettings) *)
	.settings(
			libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % Dependencies.onelibVersion % "provided")
	)

lazy val fatty = project
	.in(file("fat_files"))
	.dependsOn(core)
	.settings((Common.commonSettings ++ Common.fatSettings) *)
	.settings(
			libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % Dependencies.onelibVersion % "compile")
	)