### Running with spark-submit
````text
- Make sure there are no paths to your local filesystem used in your script!
  That's what HDFS, S3, etc. are for.
- Package up your Scala project into a JAR file(using Export in the IDE).
- Your can now use spark-submit to execute your driver script outside of the IDE.
- spark-submit --class <class object that contains your main function> \
               --jars <paths to any dependencies> \
               --files <files you want placed alongside your application>
               <your JAR file>     
               
As an example                                        
````
````console
spark-submit --class com.chema.sparkstreaming.PrintTweets \
--jars /home/chema/IdeaProjects/spark-streaming-course/dstream-twitter_2.12-0.1.0-SNAPSHOT.jar, \
       /home/chema/IdeaProjects/spark-streaming-course/twitter4j-core-4.0.4.jar, \
       /home/chema/IdeaProjects/spark-streaming-course/twitter4j-stream-4.0.4.jar \
       ClusterTweets.jar
````
