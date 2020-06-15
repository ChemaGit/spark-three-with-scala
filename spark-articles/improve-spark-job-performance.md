# Tips to improve your Apache Spark job performance!

## Making your Apache Spark application run faster with minimal changes to your code!

### Introduction
````text
While developing Spark applications, one of the most time-consuming parts was optimization. 
In this article, I’ll give some performance tips and (at least for me) 
not known configuration parameters I could have used when I’ve started.
So I’m going to cover these topics:

    - Multiple small files as source    
    - Shuffle Partitions parameter    
    - Forcing broadcast join    
    - Repartition vs Coalesce vs Shuffle partitions parameter setting
````

## What we can improve?

### Working with multiple small files?
````text
OpenCostInBytes (from documentation: [https://spark.apache.org/docs/latest/configuration.html][Spark Configuration]) — 
The estimated cost to open a file, measured by the number of bytes could be scanned at the same time. 
This is used when putting multiple files into a partition. 
It is better to over-estimated, then the partitions with small files will be faster 
than partitions with bigger files (which is scheduled first). The default value is 4MB.
````
````scala
spark.conf.set("spark.files.openCostInBytes", SOME_COST_IN_BYTES)
````
````text
I did tests on 1GB folder made up of 12k files, 
7.8GB folder from 800 files and 18GB made out of 1.6k files. 
My point was to figure out if my input files are small, 
maybe it’s better to use a lower than the default value.
````
````scala
def read_head(self) -> None:
        spark_df = (
            self.spark.read.format("csv")
            .option("header", "true")
            .option("delimiter", ",")
            .load(self.file_path + "*.csv")
        )
        new_df = spark_df.groupby("key").agg(f.mean(f.col("value")))
        print(new_df.head())
````
````text
So when testing 1GB and 7.8GB folders — winners for sure were lower values, 
but when testing ~11MB files bigger parameter values performed better.
````
##### Use openCostInBytes size which is closer to your small file sizes.
##### This will be more efficient!

### Shuffle Partitions
````text
Starting to work with Spark, I somehow got the idea, 
that the configuration I’m setting up when creating a spark session is immutable. 
Oh boy, how was I wrong.
So in general shuffle partitions are a static number (200 by default) 
in spark when you do aggregation or joins. 
Which leads to two issues based on your data size:

  - Dataset is small — 200 is way too much, data is scattered and it’s not efficient
  - Dataset is huge — 200 is way too little. 
    Data is smushed and we’re not using all resources we can as efficiently as we want.

So having some troubles with this kind of issue 
I spent a bunch of time on google and found this beautiful thing
````
````scala
spark.conf.set("spark.sql.shuffle.partitions", X)
````
````text
This neat configuration can be changed in the middle of runtime 
wherever you want and it will affect steps that are triggered after it’s set. 
You can use this bad boy when creating a spark session as well. 
This number of partitions is used when shuffling data for joins or aggregations. 
Also getting data frame partition counts:
````
````scala
df.rdd.getNumPartitions()
````
````text
You can estimate the most appropriate shuffle partition count 
for further joins and aggregations.
I.e you have one huge data frame and you want to left join some information to it. 
So you get the number of partitions of the big data frame. 
Set the shuffle partition parameter to this value. By doing so after join it won’t be the default 200! 
More parallelism — here we come!
````

### Broadcast join
````text
Very simple scenario: we have a huge table containing all our users and 
we have one which has internal ones, QA and other ones which shouldn’t be included. 
The goal is just to leave non-internal ones.
Read both tables
Huge_table left anti join small table
It looks like a straightforward and performance-wise good solution. 
If your small table is less than 10MB your small dataset will be broadcasted without any hints. 
If you add hints in your code you might get it to work on bigger datasets, but it depends on optimizer behavior.

But let’s say it’s 100–200MB and hints don’t force it to be broadcasted. 
So if you’re confident that it won’t affect the performance of your code (or throw some OOM errors) 
you can use this and override the default value:
````
````scala
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", SIZE_OF_SMALLER_DATASET)
````
````text
In this case, it will be broadcasted to all executors and join should work faster.

Beware of OOM errors!
````

### Repartition vs Coalesce vs Shuffle Partition configuration setting
````text
f you’re working with Spark you probably know the repartition method. 
For me, coming from SQL background method coalesce had way different meaning! 
Apparently in spark coalesce on partitions behaves way differently 
  — it moves and combines several partitions together. 
Basically we’re minimizing data shuffling and movement.
If we need to just decrease the number of partitions — we should probably use coalesce and not repartition, 
because it minimizes data movement and doesn’t trigger exchange. 
If we want to split our data across partitions more evenly — repartition.
But let’s say we have a recurring pattern, where we do a join/transformation and we get 200 partitions, 
but we don’t need 200, i.e. 100 or even 1. Which option of these works best?
Let’s try to compare. We’re going to read the 11MB file folder and do the same aggregation as before.
````
````scala
# reading the whole folder
big_df = (
    spark.read.format("csv")
    .option("header", "true")
    .option("delimiter", ",")
    .load("./data/csv/11MB/*.csv")
)

# Creating aggregated table
small_df = big_df.groupby("key").agg(f.mean(f.col("value")))

# checking storage size
small_df.persist(pyspark.StorageLevel.DISK_ONLY)
small_df.count()

# unpersisting 
small_df.unpersist()
````
````text
By persisting data frame on storage option only disk, we can estimate data frame size. 
So small_df is only 10 MB, but the partition count is 200. Wait what? 
It gives us 50KB per partition on average… That’s not efficient. So we’re going to read big data frame, 
and set partition count after aggregations to be 1 and to force Spark to execute we’ll do count as an action at the end.

So all in all what we can see that setting the shuffle partition parameter we don’t invoke additional 
step of Coalesce / Exchange (repartition action). So we can save some execution time by skipping it. 
If we’d look at the execution time: Shuffle Partition set finished in 7.1min, Coalesce 8.1, Repartition 8.3.
````




[Spark Configuration]: https://spark.apache.org/docs/latest/configuration.html