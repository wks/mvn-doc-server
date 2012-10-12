import sbt._
object PluginDef extends Build {
	override lazy val projects = Seq(root)
	lazy val root = Project("plugins", file(".")) dependsOn( proguardPlugin )
	lazy val proguardPlugin = uri("git://github.com/jsuereth/xsbt-proguard-plugin.git")
}
