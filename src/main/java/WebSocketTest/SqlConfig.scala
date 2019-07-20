package WebSocketTest

//想办法自动化写这些代码
object SqlConfig {

  val database = "test_server"

  val account_base_table = "account_base"

  def addComma(seq: Seq[String]): String = {
    val s = seq.map(_ + ",").reduce(_ + _)
    s.substring(0, s.length - 1)
  }

  def addParentheses(s: String): String = " (" + s + ") "

  val id = "id"
  val account_id = "account_id"
  val password: String = "password"
  val nickname = "nickname"
  val phone = "phone"
  val we_chat = "we_chat"
  val create_time = "create_time"
  val nick_change_time = "nick_change_time"

  case class AccountBaseRow(id: Int, accountId: String, password: String, nickname: String, weChat: String, phone: Long, createTime: String, nickChangeTime: String)

  val accountBaseTableCreate: (String, String) = account_base_table -> (
    //    "DROP TABLE " + accountBaseTable +
    " CREATE TABLE " +
      //      database + "." +
      account_base_table + " ( " +
      "`" + id + "` int(11) NOT NULL AUTO_INCREMENT,  " +
      "`" + account_id + "` varchar(20) NOT NULL, " +
      "`" + password + "` varchar(80) NOT NULL, " +
      " `" + nickname + "` varchar(20) NOT NULL, " +
      " `" + we_chat + "` varchar(20)," +
      " `" + phone + "` bigint(20)," +
      " `" + create_time + "`  datetime NOT NULL DEFAULT NOW()," +
      " `" + nick_change_time + "`  datetime NOT NULL DEFAULT NOW()," +
      " PRIMARY KEY (`" + id + "`)," +
      " UNIQUE KEY `" + account_id + "` (`" + account_id + "`)) " +
      "ENGINE=InnoDB DEFAULT CHARSET=utf8 "
    )


  val accountBaseInsertScheme: String = addParentheses(addComma(Seq(account_id, password, nickname, we_chat, phone)))
  val testARow = AccountBaseData("aTest", "123456", "nick", "", 12345678901L)

  case class AccountBaseData(accountId: String, password: String, nickname: String, weChat: String, phone: Long) {
    def genValue: String = {
      val seqString = Seq(accountId, JDBCLib.getSha1(password), nickname, weChat, phone.toString)
      println(JDBCLib.getSha1(password))
      " VALUES " + addParentheses(addComma(seqString.map("'" + _ + "'")))
    }

    def accountCreateSqlString: String = {

      val sql = "INSERT INTO " + SqlConfig.account_base_table + SqlConfig.accountBaseInsertScheme + this.genValue + ";"
      sql
    }

  }

  val schemaMap: Map[String, String] = Map(accountBaseTableCreate)


}
