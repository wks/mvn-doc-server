package com.github.wks.mvndocserver
import com.beust.jcommander.{ Parameter, IStringConverter }

private class ReposConverter extends IStringConverter[Seq[String]] {
  override def convert(value: String): Seq[String] = {
    value.split(":").toSeq.filterNot(_.isEmpty).map(DocServerOptions.replaceProps)
  }
}

object DocServerOptions {

  val propPattern = """%\{(.+?)\}""".r

  def replaceProps(str: String): String = {
    propPattern.replaceAllIn(str, { m => sys.props.getOrElse(m.group(1), "") })
  }

  val defaultRepos = Seq(
    "%{user.home}/.ivy2/cache",
    "%{user.home}/.m2/repository").map(replaceProps)
}

class DocServerOptions {
  import DocServerOptions._

  @Parameter(
    names = Array("-p", "--port"),
    description = "The listening port of the HTTP server.")
  var port: Int = 63787

  @Parameter(
    names = Array("-e", "--user-repos-extra"),
    description = "Paths to extra repositories, separated by ':'.",
    converter = classOf[ReposConverter])
  var userReposExtra: Seq[String] = Seq()

  @Parameter(
    names = Array("-r", "--user-repos"),
    description = "Paths to all repositories, separated by ':'. This will supress default repos.",
    converter = classOf[ReposConverter])
  var userRepos: Seq[String] = Seq()

  def repos = if (!userRepos.isEmpty) userRepos
  else userReposExtra ++ defaultRepos

}