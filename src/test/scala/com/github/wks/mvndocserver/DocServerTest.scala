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

  val repoPath = sys.props("user.home") + "/.ivy2/cache"
  val jarPath = "junit/junit/docs/junit-4.10-javadoc.jar"
  val jarFullPath = repoPath + "/" + jarPath

  val indexPath = "index.html"
  val testPath = "org/junit/Test.html"
  val testFeature = "The <code>Test</code> annotation"

  @Test
  def testJarFile {
    val jf = new JarFile(jarFullPath)
    jf.close
  }

  import DocServer.\\

  @Test
  def testFindSpecificJar {
    val fsPath = Path.fromString(repoPath)
    val urlPath = DocServer.splitPath("/%s/%s".format(jarPath, indexPath))
    val result = DocServer.openJar(fsPath, urlPath)
    result match {
      case Some(jarFile \\ innerPath) =>
        assertEquals(Seq("index.html"), innerPath)
      case None =>
        fail
    }
  }

  @Test
  def testOpenFileInJar {
    val jarFile = new JarFile("%s/%s".format(repoPath, jarPath))
    val maybeIs = DocServer.openInJarStream(jarFile, testPath)
    maybeIs match {
      case None => fail
      case Some(is) =>
        val content = Resource.fromInputStream(is).slurpString
        assertTrue(content.contains(testFeature))
        jarFile.close
    }
  }

  @Test
  def testUsingFileInJar {
    var preservedIs: java.io.InputStream = null
    DocServer.useStreamFromRepo(repoPath, DocServer.splitPath("%s/%s".format(jarPath, testPath))) { is =>
      preservedIs = is
      val content = Resource.fromInputStream(is).slurpString
      assertTrue(content.contains(testFeature))
    }

    assertEquals(0, preservedIs.available())
  }

  @Test
  def testUsingFileInJarFromRepos {
    val ds = DocServer.mkServer(Array()).get
    var preservedIs: java.io.InputStream = null
    DocServer.useStreamFromRepos(ds.repos, DocServer.splitPath("/%d/%s/%s".format(0, jarPath, testPath))) { is =>
      preservedIs = is
      val content = Resource.fromInputStream(is).slurpString
      assertTrue(content.contains(testFeature))
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
    val res1 = DocServerOptions.replaceProps("xxx%{abc}yyy")
    assertEquals("xxxdefyyy", res1)
    val res2 = DocServerOptions.replaceProps("xxx%{abd}yyy")
    assertEquals("xxxyyy", res2)
  }

  @Test
  def testDefaultRepos {
    val ds = DocServer.mkServer(Array()).get
    assertEquals(2, ds.repos.size)
  }

  @Test
  def testCustomRepos {
    for (optName <- Seq("-r", "--user-repos")) {
      val ds = DocServer.mkServer(Array(optName, "/a/b/c:/d/e/f")).get
      assertEquals(Seq("/a/b/c", "/d/e/f"), ds.repos)
    }
  }

  @Test
  def testCustomReposExtra {
    for (optName <- Seq("-e", "--user-repos-extra")) {
      val ds = DocServer.mkServer(Array(optName, "/a/b/c:/d/e/f")).get
      assertEquals(Seq("/a/b/c", "/d/e/f") ++ DocServerOptions.defaultRepos, ds.repos)
    }
  }

  @Test
  def testCustomPorts {
    for (optName <- Seq("-p", "--port")) {
      val ds = DocServer.mkServer(Array("--port", "8080")).get
      assertEquals(8080, ds.port)
    }
  }

  @Test
  def testWrongOptions {
    val dsOption = DocServer.mkServer(Array("--wrong"))
    assertEquals(None, dsOption)
  }

}