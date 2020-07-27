# Apache Spark Optimization Toolkit

## A collection of useful tips for tuning Apache Spark jobs.
````text
Apache Spark, an open-source distributed computing engine, 
is currently the most popular framework for in-memory batch-driven data processing 
(and it supports real-time data streaming as well). 
Thanks to its advanced query optimizer, DAG scheduler, and execution engine, 
Spark is able to process and analyze large datasets very efficiently. 
However, running Spark jobs without careful tuning can still lead to poor performance.
In the blog post, I will share a couple of tips for Spark performance tuning to help you 
troubleshoot and speed up slow-running Spark jobs.
(All functions mentioned in this article are from PySpark and you can find equivalent 
functions for Scala/Java using the Spark API documentation.)

https://spark.apache.org/docs/latest/api.html
````

### Uneven partitions
````text
When a dataset is initially loaded by Spark and becomes a resilient distributed dataset (RDD), 
all data is evenly distributed among partitions. However, these partitions will likely become uneven 
after users apply certain types of data manipulation to them. 
For example, the groupByKey operation can result in skewed partitions since one key might contain 
substantially more records than another. 
Moreover, because Spark’s DataFrameWriter allows writing partitioned data to disk using partitionBy, 
it is possible for on-disk partitions to be uneven as well.

Rebalancing skewed partitions in a DataFrame will tremendously improve Spark’s 
processing performance on the DataFrame. 
You can check the number of partitions in a DataFrame using the getNumPartitions function and find the 
number of records in each partition by running a light Spark job, such as:
````
````shell script
from pyspark.sql.functions import spark_partition_id
df.withColumn("partition_id", spark_partition_id())
  .groupBy("partition_id")
  .count().show()
````
````text
If you find the partition sizes of a DataFrame are highly uneven, 
use either repartition or coalesce functions to repartition 
the DataFrame before running any analysis on it. 
It is also recommended to repartition data in memory before writing them back to disk. 
The RDD module supports these repartitioning functions as well.
````

### The drawback of persisting RDD
````text
Due to the lazy evaluation principle, 
Spark does not execute any actual transformations on a dataset unless users 
explicitly call an action to collect the results. 
Moreover, if users want to apply additional transformations on the intermediate results, 
Spark will need to recompute everything from the beginning. 
To allow users to reuse date more efficiently, Spark can cache data in memory 
and/or on disk using persist or cache functions.

However, caching is not always a good idea. 
After a dataset is cached by Spark, the Catalyst optimizer’s abilities to optimize 
further transformations will be limited, because it can no longer 
improve pruning at the source data level. For instance, 
if a filter is applied to a column that is indexed in the source database, 
Catalyst will not be able to take advantage of the index to improve performance.

Thus, caching data is recommended only if it will be reused multiple times later, 
e.g. when iteratively exploring a dataset or tuning ML models.
````

### Cost-Based Optimizer (CBO)
````text
The cost-based optimizer CBO( https://docs.databricks.com/spark/latest/spark-sql/cbo.html )  
can speed up Spark SQL jobs by providing additional 
table-level statistics to Catalyst, which is especially helpful for jobs that join many datasets. 
Users can enable CBO by setting spark.sql.cbo.enabled to true (default).

To fully take advantage of the CBO, users need to keep both column-level 
and table-level statistics up-to-date, allowing CBO to optimize query plans with accurate estimates. 
To do so, use ANALYZE TABLE( https://docs.databricks.com/spark/latest/spark-sql/language-manual/analyze-table.html ) 
commands to collect statistics before running SQL queries on the tables. 
Remember to analyze tables again after tables are modified to make sure statistics are up-to-date.
````

### Broadcast Join
````text
Besides enabling CBO, another way to optimize joining datasets in Spark is by using the broadcast join. 
In a shuffle join, records from both tables will be transferred through the network to executors, 
which is suboptimal when one table is substantially bigger than the other. 
In a broadcast join, the smaller table will be sent to executors to be joined with the bigger table, 
avoiding sending a large amount of data through the network.

Users can control broadcast join via spark.sql.autoBroadcastJoinThreshold configuration, 
indicating the maximum size of tables to be broadcasted. 
Moreover, a broadcast hint (https://spark.apache.org/docs/latest/sql-performance-tuning.html#join-strategy-hints-for-sql-queries) 
can be used to tell Spark to broadcast a table even if 
the size of the table is bigger than spark.sql.autoBroadcastJoinThreshold:
````
````shell script
from pyspark.sql.functions import broadcast
broadcast(spark.table("tbl_a")).join(spark.table("tbl_b"), "key")
````

### Garbage collection (GC)
````text
As all Spark jobs are memory-intensive, 
it is important to ensure garbage collecting is effective — we want to produce less memory “garbage” 
to reduce GC time. To find out whether your Spark jobs spend too much time in GC, 
check the Task Deserialization Time and GC Time in the Spark UI.

For example, using user-defined functions (UDF) and lambda functions will lead 
to longer GC time since Spark will need to deserialize more objects. 
It is also recommended to avoiding creating intermediate objects and caching unnecessary RDDs to JVM heaps.
````

### Summary
````text
- Rebalance uneven partitions using repartition or coalesce.
- Persist data only if they will be reused for multiple times.
- Use ANALYZE TABLE commands to maintain up-to-date statistics for CBO.
- Enable broadcast join for small tables to speed up joins.
- Optimize GC by using fewer UDFs and avoiding caching large objects.
````