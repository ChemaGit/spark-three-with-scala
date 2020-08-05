package playground

object HbaseTest {

  def main(args: Array[String]): Unit = {
    import org.apache.hadoop.hbase.HBaseConfiguration

    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum","localhost")
    conf.set("hbase.rootdir","file:///Users/hadoop/dev/hbase-2.2.2/hbasestorage")
    conf.set("hbase.zookeeper.property.clientPort","2181")
    conf.set("zookeeper.znode.parent","/hbase")
    conf.set("hbase.unsafe.stream.capability.enforce","false")
    conf.set("hbase.cluster.distributed","true")

    // Create a new Hbase DB connection
    import org.apache.hadoop.hbase.client.Connection
    import org.apache.hadoop.hbase.client.ConnectionFactory
    import org.apache.hadoop.hbase.client.Put
    import org.apache.hadoop.hbase.util.Bytes

    val conn = ConnectionFactory.createConnection(conf)

    /*******************Insert Record into HBase table**************************/
    import org.apache.hadoop.hbase.TableName

    val tableName = "employee"
    val table = TableName.valueOf(tableName)
    val HbaseTable = conn.getTable(table)

    // Let set the column Families
    val cfPersonal = "personal"
    val cfProfessional = "professional"

    // Lets build the list of records to insert
    val records: List[Map[String, Any]] = List(Map("id" -> 1000,
    "name" -> "Raju Karappan",
    "city" -> "St.Augustine",
    "designation" -> "Sr.Technical Architect",
    "salary" -> 125000),
      Map("id" -> 2000,
        "name" -> "Ravi Shankar",
        "city" -> "Jacksonville",
        "designation" -> "Chief Technology Officer",
        "salary" -> 180000),
      Map("id" -> 3000,
        "name" -> "Srikiran GuruguBeli",
        "city" -> "Jacksonville",
        "designation" -> "Sr. Developer",
        "salary" -> 100000))

    // Iterate through each record to insert
    records.foreach(row => {
      // Prepare column values
      val keyValue = "Key_" + row.getOrElse("id", "NULL")
      val transRec = new Put(Bytes.toBytes(keyValue))
      val name = row.getOrElse("name", "NULL").toString
      val city = row.getOrElse("city", "NULL").toString
      val salary = row.getOrElse("salary", 0).toString
      val designation = row.getOrElse("designation", "NULL").toString

      /***********Add the specified column and value, with the specified timestamp as its version to this Put operation***************/

      // Add name to the personal column Family
      transRec.addColumn(Bytes.toBytes(cfPersonal), Bytes.toBytes("name"), Bytes.toBytes(name))
      // Add city to the personal column Family
      transRec.addColumn(Bytes.toBytes(cfPersonal), Bytes.toBytes("city"), Bytes.toBytes(city))
      // Add designation to the personal column Family
      transRec.addColumn(Bytes.toBytes(cfPersonal), Bytes.toBytes("designation"), Bytes.toBytes(designation))
      // Add salary to the personal column Family
      transRec.addColumn(Bytes.toBytes(cfPersonal), Bytes.toBytes("salary"), Bytes.toBytes(salary))

      // Insert record into HBase
      HbaseTable.put(transRec)
    })

    // Close Hbase table thread to Releases any resources held or pending changes in internal buffers.
    HbaseTable.close()
    conn.close()
  }
}
