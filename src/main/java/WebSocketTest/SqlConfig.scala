package WebSocketTest

object SqlConfig {

  val accountBase = "aTest"

  def addComma(seq: Seq[String]): String = {
    val s = seq.map(_ + ",").reduce(_ + _)
    s.substring(0, s.length - 1)
  }

  def addParentheses(s: String): String = " (" + s + ") "


  val id = "id"
  val accountId = "accountId"
  val password = "password"
  val nickname = "nickname"
  val phone = "phone"
  val test: (String, String) = accountBase -> (
    "CREATE TABLE `" + accountBase + "` ( " +
      "`" + id + "` int(11) NOT NULL AUTO_INCREMENT,  " +
      "`" + accountId + "` varchar(20) NOT NULL, " +
      "`" + password + "` varchar(20) NOT NULL, " +
      " `" + nickname + "` varchar(20) NOT NULL, " +
      //    " `sex` varchar(10) DEFAULT 'male'," +
      " `" + phone + "` int(11)," +
      " PRIMARY KEY (`id`)," +
      " UNIQUE KEY `accountId` (`accountId`)) " +
      "ENGINE=InnoDB DEFAULT CHARSET=utf8 ")


  val accountBaseInsertScheme: String = addParentheses(addComma(Seq(accountId, password, nickname, phone)))

  case class AccountBaseData(accountId: String, password: String, nickname: String, phone: Option[Int]) {
    def genValue: String = {
      val seqString = Seq(accountId, JDBCLib.getSha1(password), nickname, phone.toString)
      "VALUES " + addParentheses(addComma(seqString.map("'" + _ + "'")))

    }
  }

  val schemaMap: Map[String, String] = Map(test)


}
