An example of [GraphQL](http://graphql.org/) server built with sangria, http4s and circe which compiles and runs as a [GraalVM](https://www.graalvm.org/) native image. 

### Prerequisites

* [Docker](https://www.docker.com/community-edition)
* [Sbt](https://www.scala-sbt.org/download.html)
* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (for sbt)

### Info

The creation of the native image is done entirely with sbt. The only requirement is docker. In order to make it possible to build a native
image in any environment, I created this docker container which is used by the sbt build:

[tenshi/graalvm-native-image](https://hub.docker.com/r/tenshi/graalvm-native-image/)   

All packaging logic can be found in the `packaging.sbt`, including 2 sbt tasks: `nativeImage` and `nativeImageDocker`.

In order to compile a native image, just run:

```bash 
sbt nativeImage 
```

It will create an image in `target/native-image/sangria-http4s-graalvm-example`.

If you would like to also create a local docker container with this application image, just run:

```bash
sbt nativeImageDocker
```  

it will create a local docker container which you can run like this:

```bash
docker run -it --rm -p 8080:8080 sangria-http4s-graalvm-example server
``` 

The scala application itself has a foloing usage:

```
Usage: sangria-http4s-graalvm-example command [arguments..]

Following commands are available:

  server                Start the GraphQL server
  query <GRAPHQL_QUERY> Execute provided query and exit
```

So you can experiment with both: a CLI tool and http4s-based server.

When server is started, GraphiQL is available under http://localhost:8080 and the GraphQL endpoint itself is 
available under http://localhost:8080/graphql.   