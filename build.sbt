enablePlugins(SbtProguard)

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

proguardOptions in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

inConfig(Proguard)(javaOptions in proguard := Seq("-Xmx2g"))

//Resolving/Adding maven local projects to sbt jar
resolvers += Resolver.mavenLocal

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile, compile in Test)

enablePlugins(DockerPlugin)

dockerfile in docker := {
val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
    .mkString(":") + ":" + jarTarget

  new Dockerfile {
    from("mariadb:10.2.12")
    
    // Add all files on the classpath
    add(classpath.files, "/app/")
    
    // Add the JAR file
    add(jarFile, jarTarget)
    
    //MYCHANGES  
    //run("mkdir", "-p", "/resources")
    copy(baseDirectory(_ / "resources").value, file("/resources"))
    runRaw("""apt-get update && apt-get install -y openbabel""")
    runRaw("""apt-get update && apt-get install -y --no-install-recommends apt-utils""")
    runRaw("""apt-get update && apt-get install -y default-jre""")   	
    env("RESOURCES_HOME","/resources")
    env("VINA_DOCKING","/resources/vina")
    env("VINA_CONF","/resources/conf.txt")
    env("OBABEL_HOME","/usr/bin/obabel")
    env("SBT_OPTS","-Xmx2G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xss2M")		
    run("/resources/init_db.sh")
    expose(9000)
    //sbt -jvm-debug 9999 run
    
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

buildOptions in docker := BuildOptions(cache = false)
