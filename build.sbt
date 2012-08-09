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

import AssemblyKeys._ // put this at the top of the file

name := "mvn-doc-server"

organization := "com.github.wks"

version := "1.0"

scalaVersion := "2.9.1"

classpathTypes ~= (_ + "orbit")

libraryDependencies ++=
  "org.eclipse.jetty" % "jetty-server" % "8.1.5.v20120716" ::
  ("org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" artifacts (
    Artifact("javax.servlet", url=new java.net.URL("http://search.maven.org/remotecontent?filepath=org/eclipse/jetty/orbit/javax.servlet/3.0.0.v201112011016/javax.servlet-3.0.0.v201112011016.jar")))
  ) ::
  "org.clapper" %% "grizzled-slf4j" % "0.6.9" ::
  "ch.qos.logback" % "logback-classic" % "1.0.6" ::
  "org.specs2" %% "specs2" % "1.11" % "test" ::
  "junit" % "junit" % "4.10" % "test" ::
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.0" ::
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.0" ::
  Nil

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Scala Tools Releases" at "http://scala-tools.org/repo-releases/",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Jetty Repository" at "http://oss.sonatype.org/content/groups/jetty/"
)

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case "about.html" => MergeStrategy.discard
    case x => old(x)
  }
}
