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

import org.eclipse.jetty.server._
import org.eclipse.jetty.server.handler._
import javax.servlet.http._
import scalax.file._
import scalax.file.ImplicitConversions._
import scalax.io._
import scala.annotation.tailrec
import java.util.jar._
import java.io.InputStream
import scala.collection.GenTraversableOnce
import grizzled.slf4j.Logging
import util.control.Exception._
import resource.managed
import com.beust.jcommander.JCommander

object DocServer {

  /**
   * Split the "path" portion (from the URL) into a list of path segments.
   */
  def splitPath(pathInfo: java.lang.String): List[java.lang.String] = {
    pathInfo.split("/+").toList match {
      case "" :: xs => xs
      case xs => xs
    }
  }

  /**
   * A matcher that matches jar files
   */
  def isJavadocJar(path: Path): Boolean = path.name.endsWith("-javadoc.jar")

  /** A partial path, indicating a path inside anything else. */
  case class \\[T](t: T, insidePath: Seq[String])

  class SlashSlashSupport[T](t: T) {
    def \\(insidePath: Seq[String]) = DocServer.\\(t, insidePath)
  }

  implicit def toSlashSlashSupport[T](t: T) = new SlashSlashSupport(t)

  /**
   * Open a jar given a list of repos.
   */
  def selectRepo(repos: Seq[String], urlPath: Seq[String]): Option[\\[String]] = {
    urlPath match {
      case ri :: xs =>
        for {
          repoIndex <- allCatch.opt(ri.toInt)
          repo <- repos.lift(repoIndex)
        } yield repo \\ xs
      case _ =>
        None
    }
  }

  /**
   * Open a jar from a given path in the filesystem (probably a repo).
   */
  @tailrec
  def openJar(fsPath: Path, urlPath: Seq[String]): Option[\\[JarFile]] = {
    urlPath match {
      case _ if isJavadocJar(fsPath) => Some(new JarFile(fsPath.path) \\ urlPath)
      case x :: xs => {
        val fsPath2 = fsPath / x
        if (fsPath2.exists) openJar(fsPath2, xs)
        else None
      }
      case _ => None
    }
  }

  /**
   * Open a file as an InputStream by a given path from the jarFile. Returns None
   * if the file does not exist.
   */
  def openInJarStream(jarFile: JarFile, path: String): Option[InputStream] = {
    Option(jarFile.getEntry(path)).map(jarFile.getInputStream(_))
  }

  def useStreamFromRepos(repos: Seq[String], reqPath: Seq[String])(fn: InputStream => Unit): Boolean = {
    for {
      repo \\ inRepoPath <- selectRepo(repos, reqPath)
    } { return useStreamFromRepo(repo, inRepoPath)(fn) }
    return false
  }

  /**
   * Open an file as an InputStream by a given path inside a repo.
   * If successfully opened, the stream is automatically closed and returns true.
   * If not opened, return false.
   */
  def useStreamFromRepo(repo: String, inRepoPath: Seq[String])(fn: InputStream => Unit): Boolean = {
    for {
      jarFile_ \\ innerPath <- openJar(repo, inRepoPath)
      jarFile <- managed(jarFile_)
      is_ <- openInJarStream(jarFile, innerPath.mkString("/"))
      is <- managed(is_)
    } { fn(is); return true }
    return false
  }

  /**
   * The template of the main (jar list) page.
   */
  val mainTemplate = Resource.fromClasspath(
    "com/github/wks/mvndocserver/main.template.html",
    DocServer.getClass).slurpString("UTF-8")

  case class JarInfo(baseName: String, path: String)

  def scanRepo(repoPath: String): Seq[JarInfo] = {
    if (repoPath.isDirectory) {
      val ps = (repoPath ** "*-javadoc.jar").toSet.toSeq
      val jis = for (p <- ps) yield JarInfo(p.name, p.relativize(repoPath).path)
      jis.sortBy(_.baseName)
    } else {
      Seq()
    }
  }

  def mkServer(args: Array[String]) = {
    val opts = new DocServerOptions
    val jc = new JCommander(opts, args: _*)

    new DocServer(opts.port, opts.repos)
  }

  def main(args: Array[String]): Unit = {
    mkServer(args).run
  }
}

class DocServer(
  val port: Int,
  val repos: Seq[String]) {

  def run {
    val server = new Server(port)

    server.setHandler(handler)

    server.start
    server.join
  }

  import DocServer._

  lazy val handler = new AbstractHandler with Logging {
    override def handle(target: String, baseReq: Request,
      req: HttpServletRequest, resp: HttpServletResponse): Unit = try {

      baseReq.setHandled(true)

      val pathInfo = req.getPathInfo

      if (pathInfo == "/") {
        resp.setContentType("text/html")

        val wr = Resource.fromWriter(resp.getWriter)

        val embed =
          <div>
            {
              for ((repo, i) <- repos.zipWithIndex) yield {
                <h1>{ repo }</h1>
                <ul>
                  {
                    val jarInfos = scanRepo(repo)
                    for (ji <- jarInfos) yield {
                      <li><a href={ "/%d/%s/index.html".format(i, ji.path) }>{ ji.baseName }</a></li>
                    }
                  }
                </ul>
              }
            }
          </div>

        wr write mainTemplate.format(embed.toString)
      } else {
        useStreamFromRepos(repos, splitPath(pathInfo)) { is =>
          val os = resp.getOutputStream
          val or = Resource.fromOutputStream(os)
          val ir = Resource.fromInputStream(is)
          or.doCopyFrom(ir)
        }
      }

    } catch {
      case e => error("Error handling request", e); throw e
    }

  }
}
