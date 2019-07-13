package WebSocketTest

object SqlConfig {

  val accountBase = "aTest"

  val test = accountBase -> (
    "CREATE TABLE `" + accountBase + "` ( " +
      "`id` int(11) NOT NULL AUTO_INCREMENT,  " +
      "`accountId` varchar(20) NOT NULL, " +
      "`password` varchar(20) NOT NULL," +
      " `nickname` varchar(20) NOT NULL, " +
      " `sex` varchar(10) DEFAULT 'male'," +
      " `phone` int(11)," +
      " PRIMARY KEY (`id`)," +
      " UNIQUE KEY `accountId` (`accountId`)) " +
      "ENGINE=InnoDB DEFAULT CHARSET=utf8 ")

  //  val create
  val schemaMap: Map[String, String] = Map(test)


}
