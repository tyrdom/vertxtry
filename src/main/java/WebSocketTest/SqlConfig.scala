package WebSocketTest

object SqlConfig {

  val database = "test_server"

  val accountBaseTable = "account_base"

  def addComma(seq: Seq[String]): String = {
    val s = seq.map(_ + ",").reduce(_ + _)
    s.substring(0, s.length - 1)
  }

  def addParentheses(s: String): String = " (" + s + ") "


  val id = "id"
  val accountId = "account_id"
  val password = "password"
  val nickname = "nickname"
  val phone = "phone"
  val weChat = "we_chat"
  val createTime = "create_time"
  val nickChangeTime = "nick_change_time"
  val accountBaseTableCreate: (String, String) = accountBaseTable -> (
    //    "DROP TABLE " + accountBaseTable +
    " CREATE TABLE " +
      //      database + "." +
      accountBaseTable + " ( " +
      "`" + id + "` int(11) NOT NULL AUTO_INCREMENT,  " +
      "`" + accountId + "` varchar(20) NOT NULL, " +
      "`" + password + "` varchar(80) NOT NULL, " +
      " `" + nickname + "` varchar(20) NOT NULL, " +
      " `" + weChat + "` varchar(20)," +
      " `" + phone + "` bigint(20)," +
      " `" + createTime + "`  datetime NOT NULL DEFAULT NOW()," +
      " `" + nickChangeTime + "`  datetime NOT NULL DEFAULT NOW()," +
      " PRIMARY KEY (`" + id + "`)," +
      " UNIQUE KEY `" + accountId + "` (`" + accountId + "`)) " +
      "ENGINE=InnoDB DEFAULT CHARSET=utf8 "
    )


  val accountBaseInsertScheme: String = addParentheses(addComma(Seq(accountId, password, nickname, weChat, phone)))
  val testARow = AccountBaseData("aTest", "123456", "nick", "", 12345678901L)

  case class AccountBaseData(accountId: String, password: String, nickname: String, weChat: String, phone: Long) {
    def genValue: String = {
      val seqString = Seq(accountId, JDBCLib.getSha1(password), nickname, weChat, phone.toString)
      println(JDBCLib.getSha1(password))
      "VALUES " + addParentheses(addComma(seqString.map("'" + _ + "'")))
    }


  }

  val schemaMap: Map[String, String] = Map(accountBaseTableCreate)


}
