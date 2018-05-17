import scala.sys.process._

val nativeImage = taskKey[Unit]("Compiles GraalVM native image")
val nativeImageDocker = taskKey[Unit]("Creates a docker container with compiled GraalVM native image")

val nativeImageName = settingKey[String]("The name of the native image.")
val nativeImageResources = settingKey[Option[String]]("Regexp for resources that need to be available in the image.")
val nativeImagePath = settingKey[File]("The parent path of the native image.")
val nativeImageDockerfile = settingKey[String]("The dockerfile for a native image.")
val nativeImageDockerTags = settingKey[Seq[String]]("The docker image tags.")

retrieveManaged := true
managedDirectory := target.value / "lib_managed"
nativeImageName := name.value
nativeImagePath := target.value / "native-image"
nativeImageDockerTags := Seq(s"${nativeImageName.value}:latest", s"${nativeImageName.value}:${("git rev-parse HEAD" !!).trim}")
nativeImageResources := Some(".*graphiql.*")
nativeImageDockerfile :=
  s"""FROM ubuntu
     |
     |ADD ${nativeImageName.value} /bin/${nativeImageName.value}
     |
     |ENTRYPOINT ["/bin/${nativeImageName.value}"]
  """.stripMargin


nativeImage := {
  val baseDir = baseDirectory.value
  val s = streams.value
  val log = ProcessLogger(s.log.info(_), s.log.error(_))

  val managedDir = managedDirectory.value
  val packagedFile = Keys.`package`.in(Compile).value.relativeTo(baseDir) // make sure that package is built
  val deps = dependencyClasspath.in(Compile).value.toVector.map { dep ⇒
    dep.data.relativeTo(baseDir).orElse {
      // For some reason sbt sometimes decides to use the scala-library from `~/.sbt/boot` (which is outside of the project dir)
      // As a workaround we copy the file in lib_managed and use the copy instead (shouldn't cause name collisions)
      val inManaged = managedDir / dep.data.name
      IO.copy(Seq(dep.data → inManaged))
      inManaged.relativeTo(baseDir)
    }
  }
  val classpath = (deps :+ packagedFile).flatten
  val main = mainClass.in(Compile).value.getOrElse(sys.error("Main class is not configured"))

  val classpathStr = classpath.map(relativePath).mkString(":")
  val mountStr = absolutePath(baseDir)
  val imageParent = nativeImagePath.value
  val imagePath = imageParent / nativeImageName.value

  imageParent.mkdirs()

  val dockerProcess = Seq(
    "docker", "run", "--rm",
    "-v", s"$mountStr:/project",
    "tenshi/graalvm-native-image",
    "--verbose",
    "-cp", classpathStr,
    s"-H:Name=${relativePath(imagePath.relativeTo(baseDir).get)}",
    s"-H:Class=$main",
    "-H:+ReportUnsupportedElementsAtRuntime"
  ) ++ nativeImageResources.value.fold(Seq.empty[String])(r ⇒ Seq("-H:IncludeResources=" + r))

  Process(dockerProcess) ! log

  s.log.success("The native image is created: " + relativePath(imagePath.relativeTo(baseDir).get))
}

nativeImageDocker := {
  nativeImage.value

  val s = streams.value
  val log = ProcessLogger(s.log.info(_), s.log.error(_))

  val imageParent = nativeImagePath.value
  val imageDockerfile = nativeImagePath.value / "Dockerfile"
  val imageFile = imageParent / nativeImageName.value

  if (!imageFile.exists())
    sys.error("Native image is not yet compiled with `nativeImage`. Please do it first.")

  if (imageDockerfile.exists())
    imageDockerfile.delete()

  IO.write(imageDockerfile, nativeImageDockerfile.value)

  val tags = nativeImageDockerTags.value.flatMap(t ⇒ Seq("-t", t))

  val dockerProcess = Seq(
    Seq("docker", "build"),
    tags,
    Seq(".")).flatten

  Process(dockerProcess, imageParent) ! log

  s.log.success("Your docker image is ready! Just run:" )
  s.log.success(s"  docker run -it --rm -p 8080:8080 ${nativeImageName.value}" )
}

def relativePath(path: File): String = path.toString.replaceAll("\\\\", "/")
def absolutePath(path: File): String = {
  val parts = path.absolutePath.toString.split(":")

  // handle cygwin
  val unixPath =
    if (parts.size > 1) "/" + parts(0).toLowerCase + parts(1)
    else parts(0)

  unixPath.replaceAll("\\\\", "/")
}
