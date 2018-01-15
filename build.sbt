name := """cpvsAPI"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

lazy val buildSettings = Seq(
    organization        := "se.uu.farmbio.vs.cpvsAPI",
    version             := "0.0.1"
)

doc in Compile <<= target.map(_ / "none")

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.7.0",
  "org.jsoup" % "jsoup" % "1.11.2",
  "se.uu.farmbio" % "vs" %"0.0.1-SNAPSHOT"
)

scalacOptions += "-feature"

javaOptions ++= Seq("-Xmx2048M", "-Xms512M", "-XX:MaxPermSize=2048M")

//Resolving/Adding maven local projects to sbt jar
resolvers += Resolver.mavenLocal

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
