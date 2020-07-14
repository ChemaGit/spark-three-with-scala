package playground

import org.apache.logging.log4j.{LogManager, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.expressions.UserDefinedFunction

class SparkSerializationErrors(spark: SparkSession, nRotations: Integer) {

  import spark.implicits._

  val logger: Logger = LogManager.getLogger(getClass)

  logger.info("Initializing StringRotatorJob")

  def rotateStringUdf: UserDefinedFunction = {
    val nRotationsConst = nRotations
    udf{ str: String => SparkSerializationErrors.rotateString(str, nRotationsConst) }
  }

  def run(): Unit =
    spark
      .sql("SELECT 'Hello World!' as text")
      .withColumn("rotated_text", rotateStringUdf($"text"))
      .show()

}

object SparkSerializationErrors {

  def rotateString(str: String, nRotations: Integer): String =
    str.substring(nRotations) + str.substring(0, nRotations)
}

object Main {
  val spark = SparkSession.builder()
    .appName("SparkSerializationErrors")
    .config("spark.master", "local")
    .getOrCreate()

  def main(args: Array[String]): Unit = {
    val ssErrors = new SparkSerializationErrors(spark,6)
    ssErrors.run()
  }
}