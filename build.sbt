ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "Grenades_scala",
    idePackagePrefix := Some("dev.turtle.grenades"),
    assembly / mainClass := Some("dev.turtle.grenades.Main"),
    assembly / assemblyOutputPath := file(s"S:\\mc_server\\plugins\\${name.value}.jar"),
    ThisBuild / assemblyShadeRules := Seq(
      ShadeRule.rename("de.tr7zw.**" -> "dev.turtle.shaded.nbtapi.@1").inAll
    )
  )
resolvers ++= Seq(
  "Spigot Snapshots" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",
  "codemc-repo" at "https://repo.codemc.org/repository/maven-public/",
  "net-coreprotect" at "https://maven.playpro.com",
  "fawe/papermc" at "https://repo.papermc.io/repository/maven-public"
)

libraryDependencies ++= Seq(
  "org.spigotmc" % "spigot-api" % "1.20.2-R0.1-SNAPSHOT" % "provided",
  "de.tr7zw" % "item-nbt-api" % "2.12.0" % "compile",
  "net.coreprotect" % "coreprotect" % "21.3" % "provided",
  "com.typesafe" % "config" % "1.4.2",
  "com.fastasyncworldedit" % "FastAsyncWorldEdit-Bukkit" % "2.5.2" % "provided",
  "com.fastasyncworldedit" % "FastAsyncWorldEdit-Core" % "2.5.2" % "provided"
)