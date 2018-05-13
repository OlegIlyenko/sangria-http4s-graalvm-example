
An example of sangria + circe app compiled with [GraalVM](https://www.graalvm.org/) native-image.

Prerequisites:

* [GraalVM CE 1.0.0-rc1](https://www.graalvm.org/downloads/)
* [Sbt](https://www.scala-sbt.org/download.html)
* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (for sbt)

I prepared a script [create-native-image.sh](https://github.com/OlegIlyenko/graalvm-sangria-test/blob/master/create-native-image.sh) that you can just run to do all the steps (it compiles and packages Sbt project, executes native-image and runs the resulting executable):

```bash
chmod a+x create-native-image.sh
./create-native-image.sh
```