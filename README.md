# mvn-doc-server

Serving the HTML Javadoc files from the jars in your local directories,
including .ivy2/cache and .m2/repository.

## Building

This program is written in the Scala programming language. You also
need sbt to build this project.

Just invoke `sbt assembly`. This will create a "fat" jar that includes
all library dependencies.

I also provide pre-built jars at GitHub. Check the downloads.

## Running

Just invoke `java -jar mvn-doc-server-assembly-x.x.jar`.

## Adding additional repositories

A "repository" is a directory that may contain Javadoc jars. This
program will recursively scan all repositories for the files whose
name ends with "-javadoc.jar".

The default repositories are:

- %{user.home}/.ivy2/cache
- %{user.home}/.m2/repository

where %{user.home} will be replaced by the user's home directory.

You may set the `com.github.wks.mvndocserver.userReposExtra` property to
add additional repositories. Separate the repositories with colons.

`java -Dcom.github.wks.mvndocserver.userReposExtra=/a/b/c:/d/e/f -jar mvn-doc-server-assembly-x.x.jar`

Then `/a/b/c` and `/d/e/f` will be pre-pended before the two default
repository paths. You man also use the %{...} pattern for Java system
properties.

You may also set the `com.github.wks.mvndocserver.userRepos` property.
In this case, the default repository paths will not be used.

## Copyright

Copyright 2012 Kunshan Wang

Licensed under the Apache License, Version 2.0.

