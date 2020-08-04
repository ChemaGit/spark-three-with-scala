name := "spark-essentials"

version := "0.1"

scalaVersion := "2.12.10"

val sparkVersion = "3.0.0-preview"
val vegasVersion = "0.3.11"
val postgresVersion = "42.2.2"

resolvers ++= Seq(
  "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
  "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
  "MavenRepository" at "https://mvnrepository.com"
)


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.hbase" % "hbase-common" % "2.2.5",
  "org.apache.hbase" % "hbase-client" % "2.2.5",
  "org.apache.hbase" % "hbase-server" % "2.2.5",
  "org.apache.hbase" % "hbase-protocol" % "2.2.5",
// logging
  "org.apache.logging.log4j" % "log4j-api" % "2.4.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.4.1",
  // postgres for DB connectivity
  "org.postgresql" % "postgresql" % postgresVersion
)


