# mvn-doc-server

Serve your local JavaDocs from your Ivy or Maven repositories
(including .ivy2/cache and .m2/repository) so you can read them from
your browser.

## Introduction

If you use Maven, Ivy or Sbt, you may also download the JavaDoc and
Source packages of your dependencies.

If you use Maven or Sbt with IDEs (like Eclipse), you may already be
familiar with such packages since both Maven and Sbt have plugins that
configure those JavaDocs to be browsed within your IDEs.

This program, instead, lets you to read those documents directly
from your browser without having to unpack any of them. Sometimes it
is desired to browse the whole API of a package instead of reading
about just a single type or method. You can use this program in this
case.

## Building

This program is written in the Scala programming language. You need
Sbt to build this project.

Run `sbt proguard` to build.

The resulting jar includes all library dependencies and it is also
shrunk and optimized by proguard.

Pre-built jars are also available on GitHub. Check the downloads.

## Running

Run `java -jar mvn-doc-server_xxxxxx.jar`.

## Command-line Options

### Port

This HTTP server serves on the default port 63787. You can set this
port by setting the `-p` or `--port` command-line option.

`java -jar mvn-doc-server_xxxxxx.jar -p 8080`

The above command will serve on port 8080, instead.

### Repositories

A "repository" is a directory that may contain JavaDoc jars. This
program will recursively scan all repositories for the files whose
name ends with "-javadoc.jar".

The default repositories are:

- %{user.home}/.ivy2/cache
- %{user.home}/.m2/repository

where %{user.home} will be replaced by the user's home directory.

You may set the `-e` or `--user-repos-extra` command-line option to
add additional repositories. Separate the repositories with colons.

`java -jar mvn-doc-server_xxxxxx.jar -e /a/b/c:/d/e/f`

Then `/a/b/c` and `/d/e/f` will be pre-pended before the two default
repository paths. You man also use the %{...} pattern for Java system
properties.

You may also set the `-r` or `--user-repos` command-line option.
In this case, the default repository paths will not be used.

## Copyright

Copyright 2012 Kunshan Wang <wks1986@gmail.com>

Licensed under the Apache License, Version 2.0.

