
package com.chema.sparkstreaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel

import java.util.regex.Pattern
import java.util.regex.Matcher

import Utilities._

import java.util.concurrent._
import java.util.concurrent.atomic._

/** Monitors a stream of Apache access logs on port 9999, and prints an alarm
 *  if an excessive ratio of errors is encountered.
 */
object LogAlarmer {
  
  def main(args: Array[String]) {
    
    if(args.length != 3) {
      println(System.err.println("Usage: <window length> <host> <port>"))
      System.exit(-1)
    }
    
    val window = args(0).toInt
    val host = args(1)
    val port = args(2).toInt
    
      // Create the context with a 1 second batch size
      val ssc = new StreamingContext("local[*]", "LogAlarmer", Seconds(window))
      
      setupLogging()
      
      // Construct a regular expression (regex) to extract fields from raw Apache log lines
      val pattern = apacheLogPattern()

    
      // Create a socket stream to read log data published via netcat on port 9999 locally
      // nc -lk 9999 < /home/chema/IdeaProjects/spark-streaming-course/access_log.txt
      val lines = ssc.socketTextStream(host, port, StorageLevel.MEMORY_AND_DISK_SER)
      
      // Extract the status field from each log line
      val statuses = lines.map(x => {
            val matcher:Matcher = pattern.matcher(x); 
            if (matcher.matches()) matcher.group(6) else "[error]"
          }
      )
      
      // Now map these status results to success and failure
      val successFailure = statuses.map(x => {
        val statusCode = util.Try(x.toInt) getOrElse 0
        if (statusCode >= 200 && statusCode < 300) {
          "Success"
        } else if (statusCode >= 500 && statusCode < 600) {
          "Failure"
        } else {
          "Other"
        }
      })
      
      // Tally up statuses over a 5-minute window sliding every second
      val statusCounts = successFailure.countByValueAndWindow(Seconds(300), Seconds(1))
      
      //TODO: Improve this script by ensuring alarms are not issued more often than every half hour
      val halfAnHour = 1800000
      var currentTime = System.currentTimeMillis()
      var timePass = System.currentTimeMillis()
      
      // For each batch, get the RDD's representing data from our current window
      statusCounts.foreachRDD((rdd, time) => {    
       try {
          // Keep track of total success and error codes from each RDD
          var totalSuccess:Long = 0
          var totalError:Long = 0
    
          if (rdd.count() > 0) {        
            val elements = rdd.collect()
            for (element <- elements) {
              val result = element._1
              val count = element._2
              if (result == "Success") {
                totalSuccess += count
              }
              if (result == "Failure") {
                totalError += count
              }
            }
          }
    
          // Print totals from current window
          println("Total success: " + totalSuccess + " Total failure: " + totalError)
          
          // Don't alarm unless we have some minimum amount of data to work with
          if (totalError + totalSuccess > 100) {
            currentTime = System.currentTimeMillis()
            if(currentTime - timePass >= halfAnHour) {
              // Compute the error rate
              // Note use of util.Try to handle potential divide by zero exception
              val ratio:Double = util.Try( totalError.toDouble / totalSuccess.toDouble ) getOrElse 1.0
              // If there are more errors than successes, wake someone up
              if (ratio > 0.5) {
                // In real life, you'd use JavaMail or Scala's courier library to send an
                // email that causes somebody's phone to make annoying noises, and you'd
                // make sure these alarms are only sent at most every half hour or something.
                println("Wake somebody up! Something is horribly wrong.")
              } else {
                println("All systems go.")
              }
              currentTime = System.currentTimeMillis()
              timePass = System.currentTimeMillis()
            }
          }
       } catch {
          case j: java.net.ConnectException => System.err.println(j.getMessage)
          case e: java.lang.Exception => System.err.println(e.getMessage)      
        }      
      })
    
      // Also in real life, you'd need to monitor the case of your site freezing entirely
      // and traffic stopping. In other words, don't use this script to monitor a real
      // production website! There's more you need.
      
      // Kick it off
      ssc.checkpoint("/home/chema/IdeaProjects/spark-streaming-course/checkpoint")
      ssc.start()
      ssc.awaitTermination()
   }
}

