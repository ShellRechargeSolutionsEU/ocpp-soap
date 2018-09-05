val dispatchV = "0.11.3"
val sprayV = "1.3.4"
val specs2V = "3.9.5"
val slf4jV = "1.7.25"

val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jV
val slf4jSimple = "org.slf4j" % "slf4j-simple" % slf4jV
val dispatch = "net.databinder.dispatch" %% "dispatch-core" % dispatchV
val scalax = "com.github.t3hnar" %% "scalax" % "3.0"
val sprayHttp = "io.spray" %% "spray-http" % sprayV
val sprayHttpX = "io.spray" %% "spray-httpx" % sprayV
val akka = "com.typesafe.akka" %% "akka-actor" % "2.3.16" % "provided"
val specs2 = "org.specs2" %% "specs2-core" % specs2V % "it,test"
val specs2Mock = "org.specs2" %% "specs2-mock" % specs2V % "test"
val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
val scalaParser = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
val enumUtils = "com.thenewmotion" %% "enum-utils" % "0.2.1"
val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
val specs2ScalaCheck = "org.specs2" %% "specs2-scalacheck" % specs2V % "test"
val ocppMessages = "com.thenewmotion.ocpp" %% "ocpp-messages" % "7.0.0"

def module(name: String) = Project(name, file(name))
  .enablePlugins(OssLibPlugin)
  .configs(IntegrationTest)
  .settings(
    crossScalaVersions := Seq(tnm.ScalaVersion.prev),
    scalaVersion := tnm.ScalaVersion.prev,
    organization := "com.thenewmotion.ocpp",
    libraryDependencies ++= Seq(ocppMessages, specs2),
    Defaults.itSettings
  )

def scalaxbModule(name: String, packageNameForGeneratedCode: String) =
  module(name)
   .enablePlugins(ScalaxbPlugin)
   .settings(
     scalacOptions -="-Ywarn-unused-import",
     libraryDependencies ++= Seq(
       scalaXml,
       scalaParser,
       dispatch
     ),

     scalaxbDispatchVersion in (Compile, scalaxb) := dispatchV,
     scalaxbPackageName in (Compile, scalaxb)     := packageNameForGeneratedCode,
     // please give us good old synchronous HTTP clients for now
     scalaxbAsync in scalaxb in Compile := false,

     coverageExcludedPackages := ".*"
   )

val ocpp12Soap = scalaxbModule("ocpp-12", "com.thenewmotion.ocpp.v12")
val ocpp15Soap = scalaxbModule("ocpp-15", "com.thenewmotion.ocpp.v15")

val ocppSoap = module("ocpp-soap")
  .dependsOn(ocpp12Soap, ocpp15Soap)
  .settings(
    scalacOptions -= "-Ywarn-value-discard",
    libraryDependencies ++= Seq(
      slf4jApi, scalax, specs2Mock))

val ocppSpray = module("ocpp-spray")
  .dependsOn(ocppSoap)
  .settings(
    libraryDependencies ++= Seq(
      sprayHttp, sprayHttpX, akka, specs2Mock))

enablePlugins(OssLibPlugin)

crossScalaVersions := Seq(tnm.ScalaVersion.prev)

scalaVersion := tnm.ScalaVersion.prev

publish := {}
