# Running on a cluster

### Other spark-submit parameters
````text
--master
	- yarn - for running a YARN / HADOOP cluster
	- hostname:port - for connecting to a master on a 
	  Spark standalone
	- mesos://masternode:port
	- A master in your SparkConf will override this!!!
--num-excutors
	- Must set explicitly with YARN, only 2 by default.
--executor-memory
	- Make sure you don't try to use more memory than you have
--total-executor-cores
````

### Amazon Elastic MapReduce
````text
- A quick way to create a cluster with Spark, Hadoop, and YARN preinstalled
- You pay by the hour-instance and for network and storage IO
- Let's run our little Tweet printer on a cluster
````			  