# filet-o-fish
Research prototype for the distributed, eventually consistent, AP (as in the CAP theorem) event sourcing model.

The intended use is to play around and investigate new features (e.g. bolt-on consistency).

Written using Scala and Akka.

The central point is the `Pond.scala` file.

Various `Main` files contain some showcases for the time travel algorithm.

# Building
In order to build, make sure you have Java >= 1.8 installed and install the [`sbt` program](https://www.scala-sbt.org/download.html).
`sbt` runs on JVM, so should work "straight out of the box".

After the installation, go to the top-level directory of the project and type `sbt`. It will download its dependencies
and (after some time) offer you the following prompt: `sbt:filet-o-fish>` .

You can then type following commands:
* `compile` to compile the project
* `run` to run the project

If more than one `Main` function is present, you will be asked which of them you want to run.

Have fun!
