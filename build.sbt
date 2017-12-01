name := """cpvsAPI"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

doc in Compile <<= target.map(_ / "none")

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.7.0"
)

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)
