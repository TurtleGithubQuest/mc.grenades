import sbt.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport.*
import sbtassembly.MergeStrategy
import sbtide.Keys.idePackagePrefix

object Common {
		// Common project settings
		lazy val commonSettings: Seq[Setting[?]] = Seq(
				name := "Grenades",
				idePackagePrefix.withRank(KeyRanks.Invisible) := Some("dev.turtle.grenades"),
				assembly / mainClass := Some("dev.turtle.grenades.Main"),
				assembly / assemblyMergeStrategy := {
						case "plugin.yml" => MergeStrategy.first
						case x => (assembly / assemblyMergeStrategy).value.apply(x)
				}
		);

		// Assembly settings for a "fat" jar: include scala & shaded OneLib dependency.
		lazy val fatSettings = Seq(
				assembly / assemblyOutputPath := file(s"target/${name.value}-${version.value}-fat.jar"),
				assembly / assemblyOption ~= {
						_.withIncludeScala(true).withIncludeDependency(true)
				},
				assembly / assemblyShadeRules := Seq(
						ShadeRule.rename("de.tr7zw.**" -> "dev.turtle.shaded.nbtapi.@1").inAll,
						ShadeRule.rename("dev.turtle.onelib.**" -> "dev.turtle.shaded.onelib.@1").inAll,
				)
		)

		lazy val slimSettings = Seq(
				assembly / assemblyOutputPath := file(s"target/${name.value}-${version.value}-slim.jar"),
				assembly / assemblyOption ~= {
						_.withIncludeScala(false).withIncludeDependency(true)
				},
				assembly / assemblyShadeRules := Seq(
						ShadeRule.rename("de.tr7zw.**" -> "dev.turtle.shaded.nbtapi.@1").inAll
				)
		)

}