import sbt.Keys._

inThisBuild(List(
  organization := "com.github.stacycurl",
  homepage     := Some(url("https://github.com/stacycurl/sbt-tcr")),
  licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers   := List(
    Developer("stacycurl", "Stacy Curl", "stacy.curl@gmail.com", url("https://github.com/stacycurl"))
  ),
  usePgpKeyHex("pimpathon ci")
))

lazy val root = (project in file(".")
  settings(
    name         := "sbt-tcr",
    organization := "com.github.stacycurl",
    sbtPlugin    := true
  )

  settings(
    addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
  )
)
