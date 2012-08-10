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

import scalax.file._
import scalax.io._
import org.junit._
import org.junit.Assert._
import java.util.jar.JarFile

class DocServerTest {

  @Test
  def testMatchJar {
    assertTrue(DocServer.isJavadocJar(Path("jsoup-1.6.1-javadoc.jar")))
    assertTrue(DocServer.isJavadocJar(Path("a") / "b" / "c" / "jsoup-1.6.1-javadoc.jar"))
  }

  val repoPath = "/home/wks/.ivy2/cache"
  val jarPath = "junit/junit/docs/junit-4.10-javadoc.jar"

  val indexHtmlPath = "index.html"
  val orgJunitTestHtmlPath = "org/junit/Test.html"
  val orgJunitTestHtmlFeature = "The <code>Test</code> annotation"

  def httpReq(jp: String, fp: String) = "/" + jp + "/" + fp
  def fsJarPath(rp: String, jp: String) = rp + "/" + jp

  @Test
  def testJarFile {
    val jf = new JarFile(fsJarPath(repoPath, jarPath))
    jf.close
  }

  @Test
  def testFindSpecificJar {
    val fsPath = Path.fromString(repoPath)
    val urlPath = DocServer.splitPath(httpReq(jarPath, indexHtmlPath))
    val result = DocServer.openJar(fsPath, urlPath)
    result match {
      case Some(DocServer.InJarPath(jarFile, innerPath)) =>
        assertEquals(Seq("index.html"), innerPath)
      case None =>
        fail
    }
  }

  @Test
  def testOpenFileInJar {
    val jarFile = new JarFile(fsJarPath(repoPath, jarPath))
    val maybeIs = DocServer.openInJarStream(jarFile, orgJunitTestHtmlPath)
    maybeIs match {
      case None => fail
      case Some(is) =>
        val content = Resource.fromInputStream(is).slurpString
        assertTrue(content.contains(orgJunitTestHtmlFeature))
        jarFile.close
    }
  }

  @Test
  def testUsingFileInJar {
    var preservedIs: java.io.InputStream = null
    DocServer.useStreamFromRepo(repoPath, DocServer.splitPath(httpReq(jarPath, orgJunitTestHtmlPath))) { is =>
      preservedIs = is
      val content = Resource.fromInputStream(is).slurpString
      assertTrue(content.contains(orgJunitTestHtmlFeature))
    }

    assertEquals(0, preservedIs.available())
  }

  @Test
  def testScanRepo {
    val jis = DocServer.scanRepo(repoPath)
    for (ji <- jis) {
      assertFalse("ji.path should be relative", ji.path.contains(repoPath))
    }
  }

  @Test
  def testReplaceProps {
    sys.props.put("abc", "def")
    val res1 = DocServerConfigHelper.replaceProps("xxx%{abc}yyy")
    assertEquals("xxxdefyyy", res1)
    val res2 = DocServerConfigHelper.replaceProps("xxx%{abd}yyy")
    assertEquals("xxxyyy", res2)
  }

  @Test
  def testDefaultRepos {
    assertEquals(2, DocServerConfig.repos.size)
  }

}