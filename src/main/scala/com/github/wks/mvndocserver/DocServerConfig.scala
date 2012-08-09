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

import grizzled.slf4j.Logging
import scalax.io._
import scalax.file.Path

object DocServerConfig extends AnyRef with Logging {
  val namespace = "com.github.wks.mvndocserver"
  def opt(pn: String) = sys.props.get(namespace + "." + pn)

  val repoListFile = opt("repoListFile").getOrElse("repolist.txt")

  val propPattern = """%\{(.+?)\}""".r

  def replaceProps(str: String): String = {
    propPattern.replaceAllIn(str, { m => sys.props.getOrElse(m.group(1), "") })
  }

  val defaultRepos = Seq(
    "%{user.home}/.ivy2/cache",
    "%{user.home}/.m2/repository")

  def splitRepos(reposRepr: String) = reposRepr.split(":").filterNot(_.isEmpty).toSeq

  lazy val userRepos: Seq[String] = opt("userRepos").map(splitRepos).getOrElse(Seq())
  lazy val userReposExtra: Seq[String] = opt("userReposExtra").map(splitRepos).getOrElse(Seq())

  lazy val repos: Seq[String] = (if (userRepos.isEmpty) {
    userReposExtra ++ defaultRepos
  } else {
    userRepos
  }) map (replaceProps)
}