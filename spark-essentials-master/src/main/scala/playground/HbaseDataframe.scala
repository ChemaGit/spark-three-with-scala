package playground

import org.apache.spark.sql.SparkSession

object HbaseDataframe {

  def main(args: Array[String]): Unit = {
    import org.apache.hadoop.hbase.HBaseConfiguration

    // Create Spark Session
    val spark = SparkSession
      .builder()
      .appName("HbaseDataframe")
      .master("local[*]")
      .getOrCreate()

    // Set HBase configuration parameters using HBaseConfiguration.create() method
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", "localhost")
    conf.set("hbase.rootdir","file:///Users/hadoop/dev/apps/hbase-2.2.2/hbasestorage")
    conf.set("hbase.zookeeper.property.clientPort", "2181")
    conf.set("zookeeper.znode.parent", "/hbase")
    conf.set("hbase.unsafe.stream.capability.enforce", "false")
    conf.set("hbase.cluster.distributed", "true")

    // Create HBase connection using ConnectionFactory
    import org.apache.hadoop.hbase.client.ConnectionFactory
    import org.apache.hadoop.hbase.util.Bytes

    val conn = ConnectionFactory.createConnection(conf)

    // Create an HBase table instance
    import org.apache.hadoop.hbase.TableName
    val tableName = "employee"
    val table = TableName.valueOf(tableName)
    val HbaseTable = conn.getTable(table)

    /**
      * One of the ways to get data from HBase is to scan.
      * Scan allows iteration over multiple rows for specified attributes.
      * Initiate a client Scan instance and setup a filter criteria to retrieve
      * the rows beginning with “Key”.
      * Next add columns to be included in the result set.
      * The following is an example of a Scan on a Table instance.
      */
    // search HBase records based on the key values starting "Key"
    import org.apache.hadoop.hbase.client.Scan
    import org.apache.hadoop.hbase.filter.PrefixFilter
    import org.apache.hadoop.hbase.filter.Filter

    // Initiate a new client scanner to retrieve records
    val scan = new Scan()

    // Set the filter condition and
    val prfxValue = "Key"
    val filter: Filter = new PrefixFilter(Bytes.toBytes(prfxValue))

    // Set the filter condition
    scan.setFilter(filter)

    // Scanning the required columns
    scan.addColumn(Bytes.toBytes("personal"), Bytes.toBytes("name"))
    scan.addColumn(Bytes.toBytes("personal"), Bytes.toBytes("city"))
    scan.addColumn(Bytes.toBytes("professional"), Bytes.toBytes("designation"))
    scan.addColumn(Bytes.toBytes("professional"), Bytes.toBytes("salary"))

    // Retrieve Records
    val scanner = HbaseTable.getScanner(scan)

    /**
      * Iterate through each row and fetch data from each cell
      * and store the result set in a List[Map[String,String]] collection.
      */
    import org.apache.hadoop.hbase.util.Bytes
    import org.apache.hadoop.hbase.CellUtil

    // Iterate through the results and store the results into resValues Collection
    var resValues: List[Map[String, String]] = List()
    import scala.collection.JavaConverters._
    scanner.asScala.foreach(result => {
      var resultMap: Map[String, String] = Map()
      val cells = result.rawCells()
      for(cell <- cells) {
        val col_name = Bytes.toString(CellUtil.cloneQualifier(cell))
        val colValue = Bytes.toString(CellUtil.cloneValue(cell))
        resultMap = resultMap ++ Map(col_name -> colValue)
      }

      val resultLst = List(resultMap)
      resValues = resValues ::: resultLst
    })

    // Destroy instances of Scan, Table and Connection to release any resources held
    scanner.close()
    HbaseTable.close()
    conn.close()

    // Create a Spark DataFrame using the List[Map[String, String]]
    val colValLstMap = resValues

    // Get column names from the Map
    val colList = colValLstMap.map(x => x.keySet)

    // Get all unique columns from the list
    val uniqColList = colList.reduce((x, y) => x ++ y)

    val emptyString = ""

    // Add empty values for the non existing keys
    val newColValMap = colValLstMap.map(eleMap => {
      uniqColList.map(col => { (col, eleMap.getOrElse(col, emptyString))}).toMap
    })

    import org.apache.spark.sql.types._
    import org.apache.spark.sql.Row

    // Create your rows
    val rows = newColValMap.map(m => Row(m.values.toSeq: _*))

    // Create the schema from the header
    val header = newColValMap.head.keys.toList
    val schema = StructType(header.map(fieldName => StructField(fieldName, StringType, true)))
    val sc = spark.sparkContext

    // create your rdd
    val rdd = sc.parallelize(rows)

    // create your dataframe
    val resultDF = spark.sqlContext.createDataFrame(rdd, schema)

    resultDF.show(10, truncate = false)
  }
}
