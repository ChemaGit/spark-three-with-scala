# Tuning and Troubleshooting on a cluster
````text
- Open a browser to port 4040 on your master node
- On EMR, this involves opening up a SSH tunnel and 
  proxy server first
  - They give you instructions next to the "connections"
    item on the cluster page in the AWS console.
````

### Performance tips
````
- Use cache() or persist() if you perform multiple actions on an RDD
- Don't make your executor memory too large
- More memory and core cores better
- Think about partitioning vs. your cluster size. 
  Use repartition() when appropiate to get better parallelism
- If shrinking an RDD and repartitioning, use coalesce() to avoid
  shuffling
- Use Kryo serialization instead of Java serialization
  - conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  - Must register custom classes using:
    conf.registerKryoClasses(Array(classOf[MyClass1], classOf[MyClass2]))    
````

### Stability tips
````text
- Use Mesos or YARN
- Use Zookeeper
- Choose --executor-cores and --executor-memory wisely
- Make sure your receivers are reliable
- Look at your logs when things fail. Did a worker hang on you?
  You probably don't have enough capacity for your job, or aren't
  parallelizing things enough.
````
