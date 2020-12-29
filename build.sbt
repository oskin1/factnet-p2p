
name := "factnet-p2p"

version := "0.1"

scalaVersion := "2.13.4"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

test in assembly := {}
assemblyMergeStrategy in assembly := {
  case "logback.xml"                                             => MergeStrategy.first
  case "module-info.class"                                       => MergeStrategy.discard
  case other if other.contains("scala/annotation/nowarn.class")  => MergeStrategy.first
  case other if other.contains("scala/annotation/nowarn$.class") => MergeStrategy.first
  case other if other.contains("io.netty.versions")              => MergeStrategy.first
  case other                                                     => (assemblyMergeStrategy in assembly).value(other)
}
mainClass in assembly := Some("com.github.oskin1.factnet.Application")

libraryDependencies ++= Seq(
  "com.typesafe.akka"     %% "akka-actor"      % "2.6.10",
  "com.typesafe.akka"     %% "akka-stream"     % "2.6.10",
  "com.typesafe.akka"     %% "akka-http"       % "10.2.2",
  "io.circe"              %% "circe-core"      % "0.13.0",
  "io.circe"              %% "circe-generic"   % "0.13.0",
  "io.circe"              %% "circe-parser"    % "0.13.0",
  "org.scodec"            %% "scodec-bits"     % "1.1.21",
  "org.scodec"            %% "scodec-core"     % "1.11.7",
  "eu.timepit"            %% "refined"         % "0.9.18",
  "eu.timepit"            %% "refined-scodec"  % "0.9.18",
  "com.github.pureconfig" %% "pureconfig"      % "0.14.0",
  "com.beachape"          %% "enumeratum"      % "1.6.1",
  "ch.qos.logback"         % "logback-classic" % "1.2.3",
  "org.slf4j"              % "slf4j-api"       % "1.7.25",
  "io.chrisdavenport"     %% "log4cats-slf4j"  % "1.1.1",
  "io.github.jmcardon"    %% "tsec-hash-jca"   % "0.2.1",
  "org.scorexfoundation"  %% "scorex-util"     % "0.1.7",
  "org.scalatest"         %% "scalatest"       % "3.0.8"  % Test,
  "org.scalacheck"        %% "scalacheck"      % "1.14.1" % Test
)

lazy val allConfigDependency = "compile->compile;test->test"
