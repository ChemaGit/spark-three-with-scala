package com.chema.sparkstreaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SaveMode
import org.apache.spark.rdd.RDD

import java.util.regex.Pattern
import java.util.regex.Matcher

import Utilities._

/** Illustrates using SparkSQL with Spark Streaming, to issue queries on 
 *  Apache log data extracted from a stream on port 9999.
 *  Write the results to a JSON file
 */
object LogSQLToJson {
  
  /** Case class for converting RDD to DataFrame */
  case class Record(url: String, status: Int, agent: String)  
  
  val output = "/home/chema/IdeaProjects/spark-streaming-course/sql_json"
  
  def main(args: Array[String]): Unit = {
    if(args.length != 3) {
      println(System.err.println("Usage: <batch_size> <host> <port>"))
      System.exit(-1)
    }
    
    val batch_size = util.Try(args(0).toInt).getOrElse(2)
    val host = args(1)
    val port = util.Try(args(2).toInt).getOrElse(9999)    
    
    // Create the context with the variable batch_size
    val conf = new SparkConf()
    .setAppName("LogSQL")
    .setMaster("local[*]")
    .set("spark.sql.warehouse.dir", "/home/chema/tmp")
    
    val ssc = new StreamingContext(conf, Seconds(batch_size))    
    
    setupLogging()
    
    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()    
    
    // Create a socket stream to read log data published via netcat on port 9999 locally
    // nc -lk 9999 < /home/chema/IdeaProjects/spark-streaming-course/access_log.txt
    val lines = ssc.socketTextStream(host, port, StorageLevel.MEMORY_AND_DISK_SER)   
    
    // Extract the (URL, status, user agent) we want from each log line
    val requests = lines.map(x => {
      val matcher:Matcher = pattern.matcher(x)
      if (matcher.matches()) {
        val request = matcher.group(5)
        val requestFields = request.toString().split(" ")
        val url = util.Try(requestFields(1)) getOrElse "[error]"
        (url, matcher.group(6).toInt, matcher.group(9))
      } else {
        ("error", 0, "error")
      }
    })
    
    // Process each RDD from each batch as it comes in
    requests.foreachRDD((rdd, time) => {
      // So we'll demonstrate using SparkSQL in order to query each RDD
      // using SQL queries.

      val spark = SparkSession
         .builder()
         .appName("LogSQL")
         .getOrCreate()
         
      import spark.implicits._

      // SparkSQL can automatically create DataFrames from Scala "case classes".
      // We created the Record case class for this purpose.
      // So we'll convert each RDD of tuple data into an RDD of "Record"
      // objects, which in turn we can convert to a DataFrame using toDF()
      val requestsDataFrame = rdd.map(w => Record(w._1, w._2, w._3)).toDF()

      // Create a SQL table from this DataFrame
      requestsDataFrame.createOrReplaceTempView("requests")

      // Count up occurrences of each user agent in this RDD and print the results.
      // The powerful thing is that you can do any SQL you want here!
      // But remember it's only querying the data in this RDD, from this batch.
      val wordCountsDataFrame =
        spark.sqlContext.sql("select agent, count(*) as total from requests group by agent")
      println(s"========= $time =========")
      wordCountsDataFrame.show()
      
      // If you want to dump data into an external database instead, check out the
      // org.apache.spark.sql.DataFrameWriter class! It can write dataframes via
      // jdbc and many other formats! You can use the "append" save mode to keep
      // adding data from each batch.
      wordCountsDataFrame
      .write
      .mode(SaveMode.Append)
      .json(output)
    })
    
    // Kick it off
    ssc.checkpoint("/home/chema/IdeaProjects/spark-streaming-course/checkpoint")
    ssc.start()
    ssc.awaitTermination()    
    
  }
  
}



