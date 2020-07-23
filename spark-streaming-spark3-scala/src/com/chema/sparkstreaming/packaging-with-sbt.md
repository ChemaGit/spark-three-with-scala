# Packaging with SBT

### What is SBT
````text
- Like Maven for Scala
- Manages your library dependency tree for you
- Can package up all of your dependencies into a self-contained JAR
- If you have many dependencies(or depend on a library that in turn
  has lots of dependencies, like AWS Kinesis), it makes life a lot easier
  than passing a ton of --jars options
- Get it from scala-sbt.org
````

### Using SBT
````text
- Set up a directory structure like this:
src -> main -> scala
project

- Your Scala source files go in the source folder
- In your project folder, create an assembly.sbt file that contains on line:

	addSbtPlugin("com.eed3si9n" % "sbt-assembly" % 0.14.10)

- Check the latest sbt documentation as this will change over time.	
````

### Creating an SBT build file
````text
- At the root(alongside the src and project directories) create a build.sbt file
- Example
````
````properties
name := "wordcount"

version := "1.0"

organization := "com.sundogsoftware"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
"org.apache.spark" %% "spark-core" % "3.0.0-preview" % "provided"
)
````

### Adding dependencies
````text
- Say for example you need to depend on Kafka, which isn't built into Spark.
  You could add
  
  "org.apache.spark" %% "spark-streaming-kafka" % "1.6.1"
  
  to your library dependencies, and sbt will automatically fetch it and 
  everything it needs and bundle it into your JAR file.
- Make sure you use the correct Spark version number, and note that
  I did NOT use "provided" on the line.  
````

### Bundling it up
````text
- Just run:

  sbt assembly
  
  ...from the root folder, and it does it magic
- You'll find the JAR in target/scala-2.12 (or whatever Scala version
  you're building against.)  
  
- This JAR is self-contained! Just use spark-submit <jar file> 
  and it'll run, even without specifying a class! 
````



