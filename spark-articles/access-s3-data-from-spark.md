# How to access S3 data from Spark
````text
Getting data from an AWS S3 bucket is as easy as configuring your Spark cluster.
So you’ve decided you want to start writing a Spark job to process data. 
You’ve got your cluster created on AWS, Spark installed on those instances 
and you’ve even identified what data you want to use — it’s sitting in a bucket on AWS S3.
Now all you’ve got to do is pull that data from S3 into your Spark job. 
You could potentially use a Python library like boto3 to access your S3 bucket 
but you also could read your S3 data directly into Spark with the addition of some configuration and other parameters.
If you’ve completed the cluster installation as well as the Spark installation, there are only a few modifications 
you must make to your Spark configuration files for it to have access to AWS S3.
First, make sure that your AWS credentials are stored as user environment variables in your 
```` 
````~/.profile```` 
````text
file.
````
````shell script
export AWS_SECRET_ACCESS_KEY=XXX
export AWS_ACCESS_KEY_ID=XXX
````
````text
Add the above two lines to the ~/.profile file on all of your instances, 
and then, make sure you execute source ~/.profile on each machine to propagate those environment variables.
(VERY IMPORTANT WARNING: If you add your keys to the ~/.profile file, 
do not check this file to your Github or Bitbucket repository, otherwise 
you will compromise your AWS account by revealing these credentials to hackers and public in general.)
````

### Identify the appropriate Python AWS packages to use
````text
If you are using PySpark to access S3 buckets, 
you must pass the Spark engine the right packages to use, 
specifically aws-java-sdk and hadoop-aws. 
It’ll be important to identify the right package version to use.
As of this writing aws-java-sdk’s 1.7.4 version and hadoop-aws’s 2.7.7 version seem to work well. 
You’ll notice the maven repository for each package refer to more recent versions of both. 
It turns out the more recent versions don’t seem to work well with Spark 2.x versions, 
and so we need to use the older versions of packages to get the integration working. 
That also means that if you change the version of Spark or Hadoop you use, 
you may need to adjust version numbers for these two Python packages as well.
You can add the version for those packages to the spark-defaults.conf file 
that your Spark engine will use to configure but keep in mind any changes to that file will require you 
to stop and start your Spark engine again (/usr/local/spark/sbin/stop-all.sh and /usr/local/spark/sbin/start-all.sh). 
Instead of mucking with that configuration files, you can pass them 
to your spark-submit command using the --packages option as shown below.
````

### Run an example
````text
Here’s an example to ensure you can access data in a S3 bucket. 
Here’s some sample Spark code that runs a simple Python-based word count on a file. 
Copy that code into a file on your local master instance that is called wordcount.py 
in the below example code snippet.
````
````scala
from __future__ import print_function

import sys
from operator import add

from pyspark.sql import SparkSession


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: wordcount <file>", file=sys.stderr)
        sys.exit(-1)

    spark = SparkSession\
        .builder\
        .appName("PythonWordCount")\
        .getOrCreate()

    lines = spark.read.text(sys.argv[1]).rdd.map(lambda r: r[0])
    counts = lines.flatMap(lambda x: x.split(' ')) \
                  .map(lambda x: (x, 1)) \
                  .reduceByKey(add)
    output = counts.collect()
    for (word, count) in output:
        print("%s: %i" % (word, count))

    spark.stop()
````
````text
You’d probably want to run word count on a more interesting file but say for the purposes of testing this, 
you were interested in the Common Crawl public dataset (primarily because it’s publicly accessible) 
and wanted to know how many segments were listed in this zipped file 
(https://commoncrawl.s3.amazonaws.com/crawl-data/CC-MAIN-2020-16/segment.paths.gz). 
You could run the wordcount program and give the location of the zipped file on S3 as an input:
````
````shell script
$ spark-submit \
--packages com.amazonaws:aws-java-sdk:1.7.4,org.apache.hadoop:hadoop-aws:2.7.7 \
--master spark://MASTER_DNS:7077 wordcount.py s3a://commoncrawl/crawl-data/CC-MAIN-2020-16/segment.paths.gz
````
````text
Notice that we’ve passed the two AWS packages as an option on the command line. 
Also, remember that if you want to run Spark in a distributed manner 
(and you should if you’ve provisioned an entire cluster for its use), 
you’ll need to give to your spark-submit command an additional option 
(e.g. --master spark://MASTER_DNS:7077 option, with MASTER_DNS replaced by your master instance’s public DNS.)
The results of that command should return a list of segments in the Common Crawl, 
and look approximately something like the following:
````
````shell script
...
20/05/20 21:33:03 INFO DAGScheduler: ResultStage 1 (collect at /home/ubuntu/wordcount.py:23) finished in 0.217 s
20/05/20 21:33:03 INFO DAGScheduler: Job 0 finished: collect at /home/ubuntu/wordcount.py:23, took 9.904284 s
crawl-data/CC-MAIN-2020-16/segments/1585370490497.6/: 1
crawl-data/CC-MAIN-2020-16/segments/1585370491857.4/: 1
...
````

### V4 Signature system
````text
Finally, a word about AWS S3 buckets. In the above example, the S3 bucket can be accessed anonymously. 
However, AWS, particularly for new buckets that contain public data, 
is moving towards an authentication system that uses a V4 Signature system, 
which requires you to specify an endpoint and other options in order to access data in those buckets.
For instance, there is a newly released COVID-19 data lake that was recently made available via an AWS S3 bucket. 
Say you wanted to do a word count on the file: 
covid19-lake/static-datasets/csv/state-abv/states_abv.csv, if you run the same command
````
````shell script
$ spark-submit \
--packages com.amazonaws:aws-java-sdk:1.7.4,org.apache.hadoop:hadoop-aws:2.7.7 \
--master spark://MASTER_DNS:7077 \
wordcount.py \
s3a://covid19-lake/static-datasets/csv/state-abv/states_abv.csv
````
````text
you’d probably run into a Bad Request error:
````
````shell script
...
py4j.protocol.Py4JJavaError: An error occurred while calling o34.text.
: com.amazonaws.services.s3.model.AmazonS3Exception: Status Code: 400, 
AWS Service: Amazon S3, AWS Request ID: 9801D89CE0F1B37A, 
AWS Error Code: null, AWS Error Message: Bad Request, S3 Extended 
...
````
````text
One way to fix the error is to pass in run-time configuration flags to your spark-submit command.
Before you do that, you’d have to find the correct end point to use for the bucket. 
In this case, for the covid19-lake, the end point is s3.us-east-2.amazonaws.com.
Second, you’d also have to pass in a configuration option (com.amazonaws.services.s3.enableV4) 
to your Spark executor and driver specifically enabling V4 signature. 
Putting everything together, this is the spark-submit command you’d issue (again replace MASTER_DNS with your own values):
````
````shell script
$ spark-submit \
--packages com.amazonaws:aws-java-sdk:1.7.4,org.apache.hadoop:hadoop-aws:2.7.7 \
--conf spark.hadoop.fs.s3a.endpoint=s3.us-east-2.amazonaws.com \
--conf spark.executor.extraJavaOptions=-Dcom.amazonaws.services.s3.enableV4=true \
--conf spark.driver.extraJavaOptions=-Dcom.amazonaws.services.s3.enableV4=true \
--master spark://MASTER_DNS:7077 \
wordcount.py \
s3a://covid19-lake/static-datasets/csv/state-abv/states_abv.csv
````
````text
And if you did everything correctly, towards the bottom of the output, you should see success looking like this:
````
````shell script
...
State,Abbreviation: 1
Alabama,AL: 1
Alaska,AK: 1
Arizona,AZ: 1
Arkansas,AR: 1
````
````text
Another way to fix this issue is to add those same configuration flags to your spark-defaults.conf 
file rather than specify them on the command line. But remember you’d need to stop and start your 
Spark cluster for those configurations to take effect.
All of these examples use public data but suffice it to say that if you have your own data 
that you’ve uploaded to your own privately created S3 buckets, you can use this same exact approach to access it.
You’ve now learned the basics about accessing S3 buckets from Spark. 
Now you can go on to having fun exploring different types of datasets, including compressed file formats, 
such as Apache Parquet, or maybe explore features, such as S3 access control lists that allow you 
to manage user permissions on your data. And if you are finding that your Spark jobs are particularly slow 
pulling from S3, check out what other people have done in the past.
````