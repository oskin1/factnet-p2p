name := "factnet-p2p"

version := "0.1"

scalaVersion := "2.13.4"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.typesafe.akka"    %% "akka-actor"      % "2.6.10",
  "org.scodec"           %% "scodec-bits"     % "1.1.21",
  "org.scodec"           %% "scodec-core"     % "1.11.7",
  "eu.timepit"           %% "refined"         % "0.9.18",
  "eu.timepit"           %% "refined-scodec"  % "0.9.18",
  "com.beachape"         %% "enumeratum"      % "1.6.1",
  "ch.qos.logback"        % "logback-classic" % "1.2.3",
  "org.slf4j"             % "slf4j-api"       % "1.7.25",
  "io.chrisdavenport"    %% "log4cats-slf4j"  % "1.1.1",
  "io.github.jmcardon"   %% "tsec-hash-jca"   % "0.2.1",
  "org.scorexfoundation" %% "scorex-util"     % "0.1.7",
  "org.scalatest"        %% "scalatest"       % "3.0.8"  % Test,
  "org.scalacheck"       %% "scalacheck"      % "1.14.1" % Test
)

lazy val allConfigDependency = "compile->compile;test->test"
