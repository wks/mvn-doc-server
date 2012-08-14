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

libraryDependencies <+= sbtVersion { v =>
  // No SBT 0.11.3 support yet
  //"com.github.siasia" %% "xsbt-proguard-plugin" % (v + "-0.1.1")
  "com.github.siasia" %% "xsbt-proguard-plugin" % ("0.11.2-0.1.1")
}

libraryDependencies += "net.sf.proguard" % "proguard-base" % "4.7"
