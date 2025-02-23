import sbt.*
import sbt.Keys.*
import scala.sys.process.{Process, ProcessLogger}
import sbtassembly.AssemblyKeys.assembly
import java.nio.file.{FileSystems, Files}
import scala.jdk.CollectionConverters.*
import java.nio.file.StandardWatchEventKinds.*

object DockerTasks {
		lazy val dockerContainerName = settingKey[String]("Name of the Docker container")
		lazy val minecraftVersion = settingKey[String]("Minecraft version to use")
		lazy val dockerPluginsPath = settingKey[String]("Path to plugins in Docker container").withRank(KeyRanks.Invisible)
		lazy val assemblyProject = settingKey[String]("Project to assemble (fatty or slimmy)")

		lazy val dockerEnsureContainer = taskKey[Unit]("Ensure Docker container exists and is properly configured")
		lazy val dockerEnsureRunning = taskKey[Unit]("Ensure Docker container is running")
		lazy val deployToDocker = taskKey[Unit]("Deploy plugin to Docker container")
		lazy val assemblyAndDeploy = taskKey[Unit]("Builds and deploys the plugin")
		lazy val buildAndDeploy = taskKey[Unit]("Builds and deploys the plugin without server restart")
		lazy val codeWatch = taskKey[Unit]("Watch for changes in '/src' and redeploy")

		private def isDockerRunning: Boolean = {
				try {
						Process("docker version").! == 0
				} catch {
						case _: Exception => false
				}
		}

		private def containerExists(containerName: String): Boolean = {
				Process(s"docker container inspect $containerName").! == 0
		}

		private def startAndWaitForServerInitialization(containerName: String, log: Logger, maxAttempts: Int = 30): Boolean = {
				var serverReady = false
				var attempts = 0

				log.info("Stopping any existing server process...")
				Process(Seq(
						"docker", "exec",
						containerName,
						"/bin/bash", "-c",
						"screen -S minecraft -X stuff \"stop\\015\" || true"
				)).!

				Thread.sleep(2000)

				Process(Seq(
						"docker", "exec",
						containerName,
						"/bin/bash", "-c",
						"screen -wipe && screen -X -S minecraft quit || true"
				)).!

				log.info("Starting the server in screen session..")
				Process(Seq(
						"docker", "exec",
						containerName,
						"/bin/bash", "-c",
						"cd /home/minecraft/server && " +
							"rm -f world/session.lock && " +
							"screen -L -dmS minecraft java -jar server.jar nogui && tail -f screenlog.0"
				)).!

				log.info("Waiting for the server..")

				while (!serverReady && attempts < maxAttempts) {
						try {
								val logs = Process(Seq(
										"docker", "exec",
										containerName,
										"grep", "Done", "/home/minecraft/server/logs/latest.log"
								)).!!

								if (logs.contains("[Server thread/INFO]: Done")) {
										serverReady = true
										log.info("Server is ready!")
								} else {
										// Show last few lines of log for debugging
										Process(Seq(
												"docker", "exec",
												containerName,
												"tail", "-n", "5", "/home/minecraft/server/logs/latest.log"
										)).!(ProcessLogger(line => log.info(s"Server log: $line")))

										Thread.sleep(2000)
										attempts += 1
										if (attempts % 5 == 0) {
												log.info(s"Waiting for server... (${attempts}/${maxAttempts})")
										}
								}
						} catch {
								case e: Exception =>
										log.warn(s"Error checking server status: ${e.getMessage}")
										Thread.sleep(2000)
										attempts += 1
						}
				}
				serverReady
		}

		private def prepareBuildTools(containerName: String, minecraftVersion: String, log: Logger): Unit = {
				log.info("[BuildTools] Checking for updates of Spigot...")

				val buildCommand = Seq(
						"docker", "exec",
						containerName,
						"/bin/bash", "-c",
						"cd /home/minecraft/buildtools && " +
							"apt-get update && " +
							"apt-get install -y git curl && " +
							"curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && " +
							s"java -jar BuildTools.jar --rev $minecraftVersion --compile-if-changed"
				)

				val outputBuilder = new StringBuilder
				val logger = ProcessLogger(line => outputBuilder.append(line + "\n"))
				val exitCode = Process(buildCommand).!(logger)
				val output = outputBuilder.toString
				val noChangesDetected = output.contains("No changes detected")

				if (noChangesDetected) {
						log.info("[BuildTools] No changes detected")
						return
				}

				if (exitCode != 0) {
						sys.error("[BuildTools] Failed to build Spigot")
				}

				log.info("[BuildTools] Copying new Spigot version to server folder...")

				Process(Seq(
						"docker", "exec",
						containerName,
						"/bin/bash", "-c",
						"cp /home/minecraft/buildtools/spigot-*.jar /home/minecraft/server/server.jar"
				)).!
		}

		private def createMinecraftContainer(containerName: String, minecraftVersion: String, log: Logger): Unit = {
				log.info(s"Creating new container $containerName with Minecraft $minecraftVersion...")

				val createCommand = Seq(
						"docker", "run", "-d",
						"--name", containerName,
						"-e", "ENABLE_RCON=true",
						"-e", "RCON_PASSWORD=minecraft",
						"-p", "25565:25565",
						"-p", "25575:25575",
						"--log-driver", "json-file",
						"--log-opt", "max-size=10m",
						"--log-opt", "max-file=3",
						"eclipse-temurin:21-jdk",
						"/bin/bash", "-c",
						"mkdir -p /home/minecraft/server /home/minecraft/buildtools && " +
							"chown -R root:root /home/minecraft && " +
							"chmod -R 755 /home/minecraft && " +
							"echo 'eula=true' > /home/minecraft/server/eula.txt && " +
							"mkdir -p /home/minecraft/server/plugins && " +
							"tail -f /dev/null" // Keep container running
				)

				val exitCode = Process(createCommand).!
				if (exitCode != 0) {
						sys.error(s"Failed to create container, exit code: $exitCode")
				}
		}

		lazy val dockerSettings: Seq[Setting[?]] = Seq(
				dockerContainerName := "grenades-mc",
				minecraftVersion := "1.21",
				dockerPluginsPath := "/home/minecraft/server/plugins",

				dockerEnsureRunning := {
						val log = streams.value.log
						val containerName = dockerContainerName.value

						if (!isDockerRunning) {
								sys.error("Docker daemon is not running. Please start Docker and try again.")
						}

						val containerStatus = Process(s"docker container inspect -f '{{.State.Running}}' $containerName").!!.trim
						if (containerStatus != "true") {
								log.info(s"Starting container $containerName...")
								Process(s"docker start $containerName").!
								Thread.sleep(5000)
						}
				},

				dockerEnsureContainer := {
						val log = streams.value.log
						val containerName = dockerContainerName.value
						val mcVersion = minecraftVersion.value

						if (!containerExists(containerName)) {
								createMinecraftContainer(containerName, mcVersion, log)
						}

						dockerEnsureRunning.value
						prepareBuildTools(containerName, mcVersion, log)
				},

				deployToDocker := {
						val log = streams.value.log
						val containerName = dockerContainerName.value
						val pluginsPath = dockerPluginsPath.value

						log.info("Cleaning up existing plugin jars...")
						Process(Seq(
								"docker", "exec",
								containerName,
								"/bin/bash", "-c",
								s"rm -rf $pluginsPath/*.jar"
						)).!

						log.info("Ensuring screen package is installed...")
						Process(Seq(
								"docker", "exec",
								containerName,
								"/bin/bash", "-c",
								"apt-get update && apt-get install -y screen"
						)).!

						dockerEnsureContainer.value

						val project = assemblyProject.value
						val jarPattern = project match {
								case "fatty" => "-fat.jar"
								case "slimmy" => "-slim.jar"
						}

						val jarsToDeployDir = file("target")
						val jarToDeploy = jarsToDeployDir.listFiles()
							.find(_.getName.endsWith(jarPattern))
							.getOrElse(sys.error(s"No jar found matching pattern *$jarPattern. Available files: ${jarsToDeployDir.listFiles().mkString("Array(", ", ", ")")}"))

						log.info(s"Copying ${jarToDeploy.getName} to Docker container...")
						Process(Seq("docker", "cp", jarToDeploy.getAbsolutePath, s"$containerName:$pluginsPath/${jarToDeploy.getName}")).!
				},

				assemblyProject := sys.props.getOrElse("assemblyProject", "fatty"),


				buildAndDeploy := Def.taskDyn {
						val project = assemblyProject.value
						val assemblyTask = project match {
								case "fatty" => Def.task {
										(LocalProject("fatty") / assembly).value
								}
								case "slimmy" => Def.task {
										(LocalProject("slimmy") / assembly).value
								}
								case other => sys.error(s"Invalid project: $other. Must be 'fatty' or 'slimmy'")
						}

						Def.task {
								val jarFile = assemblyTask.value
								if (!jarFile.exists()) {
										sys.error(s"Assembly failed: jar file not produced")
								}
								deployToDocker.value
						}
				}.value,

				assemblyAndDeploy := {
						buildAndDeploy.value

						val containerName = dockerContainerName.value
						val log = streams.value.log

						if (!startAndWaitForServerInitialization(containerName, log)) {
								sys.error("Server failed to initialize within the timeout period")
						}
				},

				codeWatch := {
						val log = streams.value.log
						val containerName = dockerContainerName.value
						log.info("Starting watch service...")
						val watchService = FileSystems.getDefault.newWatchService()

						def registerAll(start: java.nio.file.Path): Unit = {
								import scala.jdk.CollectionConverters.*
								Files.walk(start).iterator().asScala.filter(Files.isDirectory(_)).foreach { dir =>
										dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
								}
						}

						val srcPath = file("src").toPath
						registerAll(srcPath)

						while (true) {
								val key = watchService.take()
								key.pollEvents().asScala.toList
								key.reset()
								log.info("Change detected, waiting for debounce period...")
								import java.util.concurrent.TimeUnit
								var polled = watchService.poll(3000L, TimeUnit.MILLISECONDS)
								while (polled != null) {
										polled.pollEvents().asScala.toList
										polled.reset()
										polled = watchService.poll(3000L, TimeUnit.MILLISECONDS)
								}
								log.info("No further changes detected, rebuilding and deploying...")
								buildAndDeploy.value //todo
								val result = Process(s"docker container inspect -f '{{.State.Running}}' $containerName").!!.trim
								if (result.toLowerCase.contains("true")) {
										log.info("Server is running, reloading plugins...")
										Process(Seq(
												"docker", "exec",
												containerName,
												"/bin/bash", "-c",
												"screen -S minecraft -X stuff \"reload\\015\""
										)).!
								} else {
										log.info("Server is not running, starting server...")
										if (!startAndWaitForServerInitialization(containerName, log)) {
												sys.error("Server failed to initialize within the timeout period")
										}
								}
						}
				}
		)

}