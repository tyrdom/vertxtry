package WebSocketTest

import java.security.MessageDigest

import WebSocketTest.Account_base.AccountBaseData
import com.mysql.cj.util.StringUtils
import io.vertx.core.AsyncResult
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.{ResultSet, SQLClient}
import com.alibaba.fastjson.JSONObject
import io.vertx.core.json.JsonObject
import msgScheme.MsgScheme.CreateAccountResponse.Reason
import scala.collection.JavaConverters._


//case class ConnectionKey(socketId: String, accountId: String)


case class ConnectionMsg(accountId: String, nickName: String, position: String, status: String, serverWebSocket: ServerWebSocket, tempPassword: Int) {
  def ChangeAccountId(newAccountId: String): ConnectionMsg = {
    ConnectionMsg(accountId = newAccountId, nickName = this.nickName, position = this.position, status = this.status, serverWebSocket = this.serverWebSocket, tempPassword = this.tempPassword)
  }

  def ChangePosition(NewPosition: String): ConnectionMsg = {
    ConnectionMsg(accountId = this.accountId, nickName = this.nickName, position = NewPosition, status = this.status, serverWebSocket = this.serverWebSocket, tempPassword = this.tempPassword)
  }

  def ChangeStatus(newStatus: String): ConnectionMsg = {
    ConnectionMsg(accountId = this.accountId, nickName = this.nickName, position = this.position, status = newStatus, serverWebSocket = this.serverWebSocket, tempPassword = this.tempPassword)
  }

  def ChangeNickname(str: String): ConnectionMsg = {
    ConnectionMsg(accountId = this.accountId, nickName = str, position = this.position, status = this.status, serverWebSocket = this.serverWebSocket, tempPassword = this.tempPassword)
  }


}

object ConnectionMsg {
  def genConnectMsgWithNoTempPassword(accountId: String, position: String, status: String, serverWebSocket: ServerWebSocket): ConnectionMsg = {
    ConnectionMsg(accountId, accountId, position, status, serverWebSocket, -1)
  }

}

object JDBCLib {

  case class ReadTestResult(var readData: Seq[JsonObject])

  def genWhereString(pairs: Seq[(String, String)]): String = {
    val s = pairs.map(aPair => {
      val where = aPair._1
      val value = aPair._2
      where + "=\"" + value + "\"" + " AND "
    }).reduce(_ + _)
    s.substring(0, s.length - 4)
  }

  def readSqlByMultiLimit(pairs: Seq[(String, String)], table: String): String
  = {

    val sql = "SELECT * FROM " + Database.database + "." + table + " WHERE " + genWhereString(pairs)
    sql
  }

  def readSqlStringARowBy1Limit(wheres: String, values: String, table: String): String
  = {

    val sql = "SELECT * FROM " + Database.database + "." + table + " WHERE " + wheres + "=\"" + values + "\"" + " LIMIT 1"
    sql
  }


  def getSha1(encryptStr: String): String = try {
    if (StringUtils.isNullOrEmpty(encryptStr)) return null
    //指定sha1算法
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(encryptStr.getBytes)
    //获取字节数组
    val messageDigest = digest.digest
    // Create Hex String
    val hexString = new StringBuffer
    // 字节数组转换为 十六进制 数
    var i = 0
    while ( {
      i < messageDigest.length
    }) {
      val shaHex = Integer.toHexString(messageDigest(i) & 0xFF)
      if (shaHex.length < 2) hexString.append(0)
      hexString.append(shaHex)

      {
        i += 1
        i - 1
      }
    }
    // 转换为全大写
    hexString.toString.toUpperCase
  } catch {
    case e: Exception =>
      e.printStackTrace()
      throw new Exception(e)
  }

  def accountCheckSqlString(account: String, password: String): String = {
    val passwordInTable = getSha1(password)
    readSqlByMultiLimit(Seq((Account_base.account_id, account), (Account_base.password, passwordInTable)), Account_base.account_base_table)

  }

  //  case class AccountBaseData(accountId: String, password: String, nickname: String, phone: Option[Int])

  case class CreateRes(ok: Boolean, reason: Reason) {
    def toJSON: JSONObject = {
      val theJsonBody: JSONObject = new JSONObject()
      theJsonBody.put("ok", ok)
      val reasonString = reason match {
        case Reason.OK => "ok"
        case Reason.NO_GOOD_PASSWORD => "NoGoodPassword"
        case _ => "other"
      }
      theJsonBody.put("reason", reasonString)
      theJsonBody
    }
  }

  def accountIdSchemeCheck(accountString: String): Boolean = accountString.length <= 15 && accountString.length >= 3 && accountString.forall(_.isLetterOrDigit) && accountString.head.isLetter

  def passwordSchemeCheck(password: String): Boolean = password.length <= 12 && password.length >= 6 && password.forall(_.isLetterOrDigit)

  def tableCheckAndCreate(jc: JDBCClient, tableName: String): Unit = {
    val sql = "DESCRIBE " + tableName
    jc.query(sql, (qryRes: AsyncResult[ResultSet]) => {
      if (qryRes.succeeded) {
        val resultSet = qryRes.result
        println("describe res:" + resultSet.getRows)
      }
      else {
        println("TableNotFound:" + tableName)
        val cSql = Account_base.schemaMap(tableName)
        println("Creating Table:" + tableName + "by Sql:" + cSql)
        jc.query(cSql, res => {
          if (res.succeeded()) {
            println("createTable:" + tableName + " ok")
          }
          else println("createTable:" + tableName + " fail,reason:" + res.cause().getMessage)
        })
      }
    }
    )
  }

  def tableCheckAllAndCreate(JDBCClient: JDBCClient): Unit = {
    Account_base.schemaMap.keys.foreach(t =>
      tableCheckAndCreate(JDBCClient, t))
  }
}
