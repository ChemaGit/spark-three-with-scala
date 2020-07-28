# How to create Spark Dataframe on HBase table
````text
Apache HBase and Hive are both data stores for storing unstructured data. 
HBase is a distributed, scalable, NoSQL big data store that runs on a Hadoop cluster. 
HBase can host very large tables — billions of rows, millions of columns — 
and can provide real-time, random read/write access to Hadoop data, 
whereas Hive is not ideally a database but a map-reduce based SQL engine that runs on top of Hadoop.

In many projects, It's common to read HBase data and process and land data into HDFS /DBs. 
Most easier and common method, many of us adapted to read Hbase is to create a Hive view 
against the Hbase table and query data using Hive Query Language or read HBase data using Spark-HBase Connector.

The caveat in using the Hive is every time you run Hive QL, 
it runs a map-reduce job to retrieve the result set. 
Better approach is to query data directly from Hbase and compute using Spark. 
In this reading let’s explore how to create spark Dataframe from Hbase database table without using 
Hive view or using Spark-HBase connector. This blog is more code oriented and the explanation 
are given as code comments.
````

### Step 1. Let’s create the following employee table in HBase Database.
````text
Given below is a sample schema of a table named “employee”. 
It has two column families: “personal” and “professional”.
````
![alt text](hbase_table.png)
````text
You can create this table using HBase shell as shown below.
````
````shell script
hbase(main):005:0> create 'employee', 'personal', 'professional'
0 row(s) in 1.2670 seconds

=> Hbase::Table - employee
hbase(main):006:0> list
TABLE

employee
````

### Step 2. Insert Data into employee table
````text
You can insert data into HBase table using Java API or HBase Shell.

- 2.1. Insert Data Using HBase Shell.
  Using put command, you can insert rows into an HBase table.
````
````shell script
hbase(main):007:0> put 'employee', '1000','personal:name','Raju Karappan'
0 row(s) in 0.2670 seconds

hbase(main):007:0> put 'employee', '1000','personal:city','St.Augustine'
0 row(s) in 0.0040 seconds

hbase(main):007:0> put 'employee', '1000','professional:designation','Sr. Technical Architect1'
0 row(s) in 0.0040 seconds

hbase(main):007:0> put 'employee', '1000','professional:salary','125000'
0 row(s) in 0.0040 seconds
````