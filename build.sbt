name := "sangria-http4s-graalvm-example"
version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-deprecation", "-feature")

mainClass in Compile := Some("Main")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2-SNAPSHOT",
  "org.sangria-graphql" %% "sangria-circe" % "1.2.1",

  "org.http4s" %% "http4s-blaze-server" % "0.18.9",
  "org.http4s" %% "http4s-circe" % "0.18.9",
  "org.http4s" %% "http4s-dsl" % "0.18.9",

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.circe" %% "circe-optics" % "0.9.3")