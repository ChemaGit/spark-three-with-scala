# Spark Essentials with Scala course

This repository contains the code I wrote during Spark Essentials with Scala course

## How to install

- install Docker
- either clone the repo or download as zip
- open with IntelliJ as an SBT project
- in a terminal window, navigate to the folder where you downloaded this repo and run `docker-compose up` to build and start the PostgreSQL container - we will interact with it from Spark
- in another terminal window, navigate to `spark-cluster/` and build the Docker-based Spark cluster with
```
chmod +x build-images.sh
./build-images.sh
```
- when prompted to start the Spark cluster, go to the `spark-cluster` folder and run `docker-compose up --scale spark-worker=3` to spin up the Spark containers


### How to start

Clone this repository and checkout the `start` tag by running the following in the repo folder:

```
git checkout start
```

### lectures

The tags are as follows:

* `start`
* `1.1-scala-recap`
* `2.1-dataframes`
* `2.2-dataframes-basics-exercise`
* `2.4-datasources`
* `2.5-datasources-part-2`
* `2.6-columns-expressions`
* `2.7-columns-expressions-exercise`
* `2.8-aggregations`
* `2.9-joins`
* `2.10-joins-exercise`
* `3.1-common-types`
* `3.2-complex-types`
* `3.3-managing-nulls`
* `3.4-datasets`
* `3.5-datasets-part-2`
* `4.1-spark-sql-shell`
* `4.2-spark-sql`
* `4.3-spark-sql-exercises`
* `5.1-rdds`
* `5.2-rdds-part-2`

And for premium students, in addition:

* `6.1-spark-job-anatomy`
* `6.2-deploying-to-cluster`
* `7.1-taxi`
* `7.2-taxi-2`
* `7.3-taxi-3`
* `7.4-taxi-4`