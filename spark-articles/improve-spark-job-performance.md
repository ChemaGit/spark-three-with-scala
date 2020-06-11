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


[Spark Configuration]: https://spark.apache.org/docs/latest/configuration.html