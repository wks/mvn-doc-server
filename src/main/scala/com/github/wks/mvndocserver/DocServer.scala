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

  case class InRepoPath(repo: String, inRepoPath: Seq[String])

  /**
   * Open a jar given a list of repos.
   */
  def selectRepo(urlPath: Seq[String]): Option[InRepoPath] = {
    urlPath match {
      case ri :: xs =>
        for {
          repoIndex <- allCatch.opt(ri.toInt)
          repo <- repos.lift(repoIndex)
        } yield InRepoPath(repo, xs)
      case _ =>
        None
    }
  }

  /**
   * Result of opening a path. The jar file and the path inside the jar.
   */
  case class InJarPath(val jarFile: JarFile, val innerPath: Seq[String])

  /**
   * Open a jar from a given path in the filesystem (probably a repo).
   */
  @tailrec
  def openJar(fsPath: Path, urlPath: Seq[String]): Option[InJarPath] = {
    urlPath match {
      case _ if isJavadocJar(fsPath) => Some(InJarPath(new JarFile(fsPath.path), urlPath))
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

  /**
   * Ensure a closeable object is closed after the block.
   */
  def autoClose[T](closeable: { def close() })(fn: => T): T = {
    try {
      fn
    } finally {
      closeable.close
    }
  }

  def useStreamFromRepos(reqPath: Seq[String])(fn: InputStream => Unit): Boolean = {
    (for {
      InRepoPath(repo, inRepoPath) <- selectRepo(reqPath)
    } yield useStreamFromRepo(repo, inRepoPath)(fn)).getOrElse(false)
  }

  /**
   * Open an file as an InputStream by a given path inside a repo.
   * If successfully opened, the stream is automatically closed and returns true.
   * If not opened, return false.
   */
  def useStreamFromRepo(repo: String, inRepoPath: Seq[String])(fn: InputStream => Unit): Boolean = {
    openJar(repo, inRepoPath) match {
      case None => false
      case Some(InJarPath(jarFile, innerPath)) =>
        autoClose(jarFile) {
          openInJarStream(jarFile, innerPath.mkString("/")) match {
            case None => false
            case Some(is) =>
              autoClose(is) {
                fn(is)
                true
              }
          }
        }
    }
  }

  /**
   * The template of the main (jar list) page.
   */
  val mainTemplate = Resource.fromClasspath(
    "com/github/wks/mvndocserver/main.template.html",
    DocServer.getClass).slurpString("UTF-8")

  /**
   * The list of repositories.
   */
  val repos = DocServerConfig.repos

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
        useStreamFromRepos(splitPath(pathInfo)) { is =>
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

  def main(args: Array[String]): Unit = {
    val server = new Server(8080)

    server.setHandler(handler)

    server.start
    server.join

  }
}