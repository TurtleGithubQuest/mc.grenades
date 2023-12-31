ThisBuild / version := "1.9"
ThisBuild / scalaVersion := "3.3.1"
ThisBuild / resolvers ++= Seq(
  "Jitpack" at "https://jitpack.io",
  "Spigot Snapshots" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",
  "codemc-repo" at "https://repo.codemc.org/repository/maven-public/",
  "net-coreprotect" at "https://maven.playpro.com"
)
ThisBuild / libraryDependencies ++= Seq(
  "org.spigotmc" % "spigot-api" % "1.20.2-R0.1-SNAPSHOT" % "provided",
  "de.tr7zw" % "item-nbt-api" % "2.12.0" % "compile",
  "net.coreprotect" % "coreprotect" % "21.3" % "provided",
  "com.typesafe" % "config" % "1.4.2"
)
lazy val onelib_version = "1.0.1b"
lazy val commonSettings = Seq(
  name := "Grenades",
  idePackagePrefix.withRank(KeyRanks.Invisible) := Some("dev.turtle.grenades"),
  assembly / mainClass := Some("dev.turtle.grenades.Main"),
  assembly / assemblyMergeStrategy := {
    case "plugin.yml" => MergeStrategy.first
    case x => (assembly / assemblyMergeStrategy).value.apply(x)
  },
)
lazy val fatSettings = Seq(
  assembly / assemblyOutputPath := file(s"S:\\mc_server\\plugins\\${name.value}-${version.value}-fat.jar"),
  assembly / assemblyOption ~= {
    _.withIncludeScala(true).withIncludeDependency(true)
  },
  assembly / assemblyShadeRules := Seq(
    ShadeRule.rename("de.tr7zw.**" -> "dev.turtle.shaded.nbtapi.@1").inAll,
    ShadeRule.rename("dev.turtle.onelib.**" -> "dev.turtle.shaded.onelib.@1").inAll,
  )
)
lazy val slimSettings = Seq(
  assembly / assemblyOutputPath := file(s"S:\\mc_server\\plugins\\${name.value}-${version.value}-slim.jar"),
  assembly / assemblyOption ~= {
    _.withIncludeScala(false).withIncludeDependency(true)
  },
  assembly / assemblyShadeRules := Seq(
    ShadeRule.rename("de.tr7zw.**" -> "dev.turtle.shaded.nbtapi.@1").inAll
  )
)
/*inConfig(Fat)(baseAssemblySettings ++ inTask(assembly)(fatSettings))
inConfig(Slim)(baseAssemblySettings ++ inTask(assembly)(slimSettings))*/
lazy val core = project.in(file("."))
  .settings(libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % onelib_version % "provided"))
  .settings(commonSettings:_*)
/**
 *  slimmy / assembly
 *  @return no shaded OneLib & Scala libs
 */
lazy val slimmy = Project(
  id="slimmy",
  base=file("slim_files")
).dependsOn(core)
  .settings(
  libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % onelib_version % "provided"),
).settings(commonSettings++slimSettings)
/**
 *  fatty / assembly
 *  @return shaded OneLib & Scala, "standalone" ver
 */
lazy val fatty = Project(
  id="fatty",
  base=file("fat_files")
).dependsOn(core)
  .settings(
  libraryDependencies ++= Seq("com.github.TurtleGithubQuest" % "OneLib" % onelib_version % "compile"),
).settings(commonSettings++fatSettings)