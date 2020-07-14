package playground

import java.util

import org.apache.spark.sql.SparkSession
import java.util.ArrayList
import scala.collection.immutable.List

object Casting {

  val spark = SparkSession.builder()
    .appName("Casting")
    .config("spark.master", "local[*]")
    .getOrCreate()

  case class MyObject2(fname: String, age: Int)

  // case class MyObject(key: String, dataList: ArrayList[MyObject2])
  case class MyObject(key: String, dataList: List[MyObject2])

  val arrLst = new util.ArrayList[MyObject2]()
  arrLst.add(MyObject2("Marie", 25))
  arrLst.add(MyObject2("Peter", 27))

  val lst = List(MyObject2("Marie", 25),MyObject2("Peter", 27))

  val data: Seq[MyObject] = Seq(MyObject("Lucia",lst))

  def main(args: Array[String]): Unit = {

    import spark.implicits._

    try {
      // val df = spark.createDataFrame(data)

      val df1 = spark.sparkContext.parallelize(data).toDS()

      df1.printSchema()
      df1.map(row => {
        val key  = row.key
        val values = row.dataList
        (key, values)
      }).show(false)

      //df.show()
    } finally {
      spark.stop()
      println("Spark Session has stopped.")
    }
  }
}

/*
case class MyObject(key: String, dataList: List[MyObject2])
 */