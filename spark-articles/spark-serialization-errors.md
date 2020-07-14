# Spark Serialization Errors
````text
This notorious error has caused persistent frustration for Spark developers:
````
````scala
org.apache.spark.SparkException: Task not serializable
````

````text
Along with this message, Spark spits out a chain of garbled class and method names, 
trying to give some hint of the cause. Searching for answers online yields several solutions, 
but every situation is different. Some combination of these tactics might solve your problem, others will not.

This Stack Overflow post:

https://stackoverflow.com/questions/25914057/task-not-serializable-exception-while-running-apache-spark-job/43015968#43015968

presents a good summary of relevant information. 
However, like other answers online, the reader walks away with a list of “DOs and DONTs,” 
still missing a deeper understanding of what has gone wrong.

Let’s dive deeper into the issue with a simple example, and explore each piece of the problem. 
By drilling down into what Spark is doing under the hood, we can develop a methodical approach to this problem, rather than a trial-and-error checklist.
````

### A String Rotator UDF
````text
Serialization errors frequently arise from UDFs. 
For example, suppose we want to use a UDF to rotate the strings in some text column by 7 places, 
and print the results to the console. A simple (but ugly) implementation looks like this:
````
````scala
class StringRotatorJob(spark: SparkSession) {

  import spark.implicits._

  def rotateStringUdf: UserDefinedFunction =
    udf { str: String => str.substring(7) + str.substring(0, 7) }

  def run(): Unit =
    spark
      .sql("SELECT 'Hello World!' as text")
      .withColumn("rotated_text", rotateStringUdf($"text"))
      .show()

}
````
````
And the output:
````
````
+------------+------------+
|        text|rotated_text|
+------------+------------+
|Hello World!|World!Hello |
+------------+------------+
````

````text
Great. We are very proud of the cutting edge 
string rotation technology we’ve produced here.

However, when we’re demoing our invention, our teammates insist 
that our code is too complex and coupled, and that we need to break up 
our monolithic monstrosity into smaller functions to improve readability. 
Specifically, our string rotating operation is far too complex to be inlined, 
the number of places to rotate the string by should be a parameter of the job, 
and the function should be extracted out of the UDF, 
into a separate method, so that we can write a unit test for it.

We want everyone to like us, so we refactor.
````
````scala
class StringRotatorJob(spark: SparkSession, nRotations: Integer) {

  import spark.implicits._

  def rotateString(str: String): String =
    str.substring(nRotations) + str.substring(0, nRotations)

  def rotateStringUdf: UserDefinedFunction =
    udf { str: String => rotateString(str) }

  def run(): Unit =
    spark
      .sql("SELECT 'Hello World!' as text")
      .withColumn("rotated_text", rotateStringUdf($"text"))
      .show()

}
````
````
The code quality overlords are appeased, but our code doesn’t run anymore:
````
````scala
org.apache.spark.SparkException: Task not serializable

Caused by: java.io.NotSerializableException: StringRotatorJob
Serialization stack:
 - object not serializable (class: StringRotatorJob, value: StringRotatorJob@2a0a5865)
 - field (class: StringRotatorJob$$anonfun$rotateStringUdf$1, name: $outer, type: class StringRotatorJob)
 - object (class StringRotatorJob$$anonfun$rotateStringUdf$1, <function1>)
 - element of array (index: 4)
 - array (class [Ljava.lang.Object;, size 5)
 - field (class: org.apache.spark.sql.execution.WholeStageCodegenExec$$anonfun$13, name: references$1, type: class [Ljava.lang.Object;)
 - object (class org.apache.spark.sql.execution.WholeStageCodegenExec$$anonfun$13, <function2>)
````

### Mitigation Attempts
````text
After pushing through some stages of grief, we finally overcome 
our demoralization and seek help online, finding comfort on Stack Overflow. 
With 30 tabs open in our browser, we attempt a trial-and-error approach 
with some of the proposed techniques.
````

#### 1. Use a static method
````text
We can move our super-modularized reverseString method to a separate class, 
or even make it static by moving it to a companion object.
````
````scala
class StringRotatorJob(spark: SparkSession, nRotations: Integer) {

  def rotateStringUdf: UserDefinedFunction =
    udf { str: String =>
      StringRotatorJob.rotateString(str, nRotations)
    }
}
object StringRotatorJob {
  def rotateString(str: String, nRotations: Integer): String =
    str.substring(nRotations) + str.substring(0, nRotations)
}
````
````text
Unfortunately, the error persists.
````

#### 2. Declare a Constant
````text
This option looks like black-magic. 
It involves redeclaring nRotations as a constant.
````
````scala
def rotateStringUdf: UserDefinedFunction = {
  val nRotationsConst = nRotations
  udf { str: String => rotateString(str, nRotationsConst) }
}
````
````text
Again, no luck.
````

#### 3. Implement Serializable
````text
StringReverserJob isn’t serializable? Let’s FORCE it to be.
````
````scala
class StringRotatorJob(spark: SparkSession, nRotations: Integer)
  extends Serializable {
  
  // ...
}
````
````text
It works! And more importantly, it feels reasonable. 
A serialization error occurred, so we made something Serializable. 
We can almost even convince ourselves that we understand the problem and our solution.
````

#### Hanging by a Thread
````text
We will eventually discover that the solution we chose is delicately hanging by a thread. 
A simple change, such introducing a Log4j logger into the class, throws us back into the flames.
This minor change:
````
````scala
class StringRotatorJob(spark: SparkSession, nRotations: Integer)
  extends Serializable {

  val logger: Logger = LogManager.getLogger(getClass)
  
  logger.info("Initializing StringRotatorJob")
  // ...
}
````
````text
Results in the return of the error.
````
````shell script
org.apache.spark.SparkException: Task not serializable

Caused by: java.io.NotSerializableException: org.apache.log4j.Logger
Serialization stack:
 - object not serializable (class: org.apache.log4j.Logger, value: org.apache.log4j.Logger@6d49bd08)
 - field (class: StringRotatorJob, name: logger, type: class org.apache.log4j.Logger)
 - object (class StringRotatorJob, StringRotatorJob@249a7ccc)
 - field (class: StringRotatorJob$$anonfun$rotateStringUdf$1, name: $outer, type: class StringRotatorJob)
 - object (class StringRotatorJob$$anonfun$rotateStringUdf$1, <function1>)
 - element of array (index: 4)
 - array (class [Ljava.lang.Object;, size 5)
 - field (class: org.apache.spark.sql.execution.WholeStageCodegenExec$$anonfun$13, name: references$1, type: class [Ljava.lang.Object;)
 - object (class org.apache.spark.sql.execution.WholeStageCodegenExec$$anonfun$13, <function2>)
````
````text
After another round of trial-and-error, 
we find that combining the other two techniques satisfies Spark, 
and the error subside. We ultimately land at this solution:
````
````scala
class StringRotatorJob(spark: SparkSession, nRotations: Integer) {

  import spark.implicits._

  val logger: Logger = LogManager.getLogger(getClass)

  logger.info("Initializing StringRotatorJob")

  def rotateStringUdf: UserDefinedFunction = {
    val nRotationsConst = nRotations
    udf { str: String =>
      StringRotatorJob.rotateString(str, nRotationsConst)
    }
  }

  def run(): Unit =
    spark
      .sql("SELECT 'Hello World!' as text")
      .withColumn("rotated_text", rotateStringUdf($"text"))
      .show()

}

object StringRotatorJob {

  def rotateString(str: String, nRotations: Integer): String =
    str.substring(nRotations) + str.substring(0, nRotations)

}
````

````text
The only thing more frustrating than the Task not serializable error itself 
is the fact that we do not understand the solution.
To demystify the problem, we must break it down and zoom in on each piece.
Why are things being serialized? 
This relates to what Spark is — a distributed processing engine.
What exactly is being serialized? To answer this, we need to understand how functions are represented in the JVM.
Why is the end result not serializable? This requires some analysis of how Spark serializes objects.
````

### Why serialization?
````text
Spark is a distributed processing system consisting of a driver node and worker nodes. 
When our program starts up, our compiled code is loaded by all of these nodes. 
Driver and worker alike, everyone receives a copy of the compiled classes.

This is possible because classes are static; 
they are set in stone from the moment the code is compiled. 
So a copy can be delivered to each node, driver or worker, 
without requiring any serialization to occur at runtime.

Once our job begins, the driver node runs some initialization 
before the workers begin executing our job:

    1. It instantiates a StringRotatorJob object. 
       This involves initialization code, 
       such as storing the spark and nRotations fields 
       from the constructor parameters, creating a logger instance, 
       constructing our UDF, etc.

    2. It builds (not executes) a SQL query that will select 
       the literal string “Hello World!” then apply our UDF to it. 
       To reiterate, this step merely builds instructions 
       of what the workers will later execute.

The reference that is the root of our troubles is 
the function we have passed to udf(). This function is
````
````scala
str: String => rotateString(str)
````
````text
This function appears to be a simple wrapper around another function:
````
````scala
def rotateString(str: String): String =
    str.substring(nRotations) + str.substring(0, nRotations)
````
````text
The functions referenced by the UDF appear to be extremely simple, 
and entirely stateless. Why should Spark have any trouble serializing them? 
For this, we have to talk about how Java models functions.
````

### Functions in the JVM

#### Does Java Have Functions?
````text
No. Well, yes — well, sort of.
Spark is written in Scala, which is a sibling of Java. 
Both languages are compiled to Java bytecode and run on the JVM. 
When asking questions that touch on lower-level concepts, like “do functions exist,” 
we have to remember what lies under the hood: the JVM.

To be precise: No, Java does not have functions, 
at least not as a part of the core language. 
Java has objects, and objects have methods. 
A method is like a function, except that a special object 
is always present in the scope. Its fields and other methods can be 
referenced implicitly by invoking them without any named target, 
or explicitly using the target this.

Thus, the “simple, stateless” function we are passing to our UDF:
````
````scala
str: String => rotateString(str)
````
````text
is concealing an implicit reference to a stateful object called this. 
Writing it out more explicitly, our “function” actually looks like:
````
````scala
str: String => this.rotateString(str)
````

### Functional Interfaces
````text
So if Java does not have functions, then what about the anonymous function we passed to the UDF? 
````
````scala
str: String => rotateString(str)
````

````text
Again, Scala is just Java under the hood, and Java does not have functions, 
only methods, which must be attached to objects. 
Scala introduced a functional programming syntax, using the => symbol. 
Later on, Java 8 similarly introduced lambdas, using the -> symbol. 
But these are just syntactic illusions, abstractions built 
on top of objects and methods for the purpose of reducing boilerplate code.

For some background context, remember that there was a time before 
Scala was invented (and before Java 8), when developers did not have these 
“functional programming” paradigms available in the language. 
When a function needed to be stored in a variable and passed around, 
the solution was more verbose: a function was modeled as an object with one method. 
For example, a simple “increment” function, which would today be written as
````
````java
x -> x + 1
````
````text
would have been written like this:
````
````java
public class Incrementer {
    public int apply(int x) {
        return x + 1;
    }
}
````
````text
The sleek functional syntax introduced by Scala and Java 8 only make you feel like 
you’re writing functions. Under the hood, this is just shorthand — syntactic sugar — 
for what still compiles to JVM bytecode as an object with one method. 
So when we write x -> x + 1 in Java 8, or x => x + 1 in Scala, 
this ultimately compiles to the same bytecode as the Incrementer class shown above.

Both of these newer languages model these patterns using interfaces. 
Java 8 calls these Functional Interfaces. Scala has similar interfaces, 
such as Function1, Function2, etc. The shorthand syntax, when compiled, 
automatically generates anonymous classes that implement these interfaces. 
For anyone out there who enjoys reading bytecode, here are some references 
analyzing this bytecode in depth both for Scala and for Java 8.
````

### Closures
````text
A closure is a function that accesses variables defined outside of its local scope, 
i.e. in the “outer scope.” Its name comes from the idea that it “closes around” the outer scope.

If you search around, you can find an unlimited supply of articles refining this definition, 
using fancy terms like “lexical scope” and “execution context.” Let’s not go down that road. 
Just recognize that our anonymous function str: String => this.rotateString(str) is accessing 
a variable this which is not defined within its scope. That makes the function a closure.

Given that Java models a function as an object with one method, 
there is a very natural extension to this, which allows it to easily model closures: 
the variables from the outer scope are stored as attributes on the object. 
Therefore, the anonymous function we have passed to our UDF is equivalent, after compilation, to this:
````
````java
public class StringRotatorFunction {

    private StringRotatorJob job;

    public StringRotatorFunction(StringRotatorJob job) {
        this.job = job;
    }

    public String rotateString(String str) {
        return str.substring(job.nRotations) +
               str.substring(0, job.nRotations);
    }

}
````
````text
Why did we veer off on this tangent? 
Because this 👆 is essentially the object being serialized by Spark.
````

### Object Serialization
````text
Now that we know what Spark is trying to serialize and why, we can deduce where the error is occurring.

Spark’s serialization strategy is outlined in its documentation: 
https://spark.apache.org/docs/latest/tuning.html#data-serialization

By default, Spark serializes objects using Java’s ObjectOutputStream framework, 
and can work with any class you create that implements java.io.Serializable.

This means that Spark serializes an object by calling ObjectOutputStream.writeObject() on it. 
In order for this to work, the class must implement the Serializable interface:
https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html

Serializability of a class is enabled by the class implementing the java.io.Serializable interface. 
Classes that do not implement this interface will not have any of their state 
serialized or deserialized…The serialization interface has no methods or fields 
and serves only to identify the semantics of being serializable.

An object can hold references to other objects, inducing an “object graph.” 
The documentation: 
https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/ObjectOutputStream.html#writeObject(java.lang.Object)  
for ObjectOutputStream explains how this graph is serialized, 
with writeObject() being called recursively on each member held by the original object:

Objects referenced by this object are written transitively so that 
a complete equivalent graph of objects can be reconstructed by an ObjectInputStream.

The documentation for Serializable warns us of the outcome 
if any object in this graph does not implement Serializable:

When traversing a graph, an object may be encountered that does not support the Serializable interface. 
In this case the NotSerializableException will be thrown and will identify the class of the non-serializable object.

And this, here, is the root of our problem. 
Our UDF holds a function that must be serialized. 
This function is actually an object, which contains a reference to a StringRotatorJob, 
which in turn references a Logger, which does not implement Serializable.
````

### Solution
````text
We have reframed the problem: Our function is a graph of objects, 
where some of those objects are not serializable. 
For each non-serializable node on this graph, we have two options:

  1. Make the node implement Serializable.
  2. 

Of the 3 tactics described earlier, each is a specific way of achieving one of these options:

  1. Use a static methods (detach)
  2. Declare a constant (detach)
  3. Implement Serializable

While 1 and 2 solve the problem for a node and all of its children, 
number 3 has a catch: this node is now serializable, but its children still might not be 
(for example, making StringRotatorJob serializable did not help us when its child Logger was not).

There are more mechanisms available. 
However, rather than applying a combination of pre-established tactics until our code runs, 
we should aim to identify the object graph, and determine which nodes should and shouldn’t be serialized.

For example, in our situation, StringRotatorJob has no business being sent to workers, 
or being serialized for any other purpose. Rather than forcing it to implement Serializable, 
we should detach our UDF from any references to it.

But in some other situation, we might have an object which acts as a data or parameter holder, 
which is inextricably tied to our function. This class should implement Serializable. 
Furthermore, this communicates to other developers that this class is intended to be serialized, 
and should remain free of any attributes which compromise this purpose.
````


