//   Copyright 2012 Kunshan Wang
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
package com.github.wks.mvndocserver

object DocServerOptions {
  def parser(c: DocServerOptions) = new scopt.mutable.OptionParser("mvn-doc-server", "1.1") {
      intOpt("p", "port", "Server listening port",
          v => c.port = v)
      opt("e", "user-repos-extra", "Paths to extra repositories, separated by ':'.",
          v => c.userReposExtra = parseRepos(v))
      opt("r", "user-repos", "Paths to repositories, separated by ':'. Replace default repos",
          v => c.userRepos = parseRepos(v))
  }

  val propPattern = """%\{(.+?)\}""".r

  def replaceProps(str: String): String = {
    propPattern.replaceAllIn(str, { m => sys.props.getOrElse(m.group(1), "") })
  }
  
  def parseRepos(str:String) = str.split(":").toSeq.filterNot(_.isEmpty).map(replaceProps)

  val defaultPort = 63787

  val defaultRepos = Seq(
    "%{user.home}/.ivy2/cache",
    "%{user.home}/.m2/repository").map(replaceProps)
}

class DocServerOptions {
  var port: Int = 63787
  var userReposExtra: Seq[String] = Seq()
  var userRepos: Seq[String] = Seq()
  
  import DocServerOptions.defaultRepos

  def repos = if (!userRepos.isEmpty) userRepos
  else userReposExtra ++ defaultRepos

}