package WebSocketTest

import java.security.MessageDigest

import WebSocketTest.SqlConfig.AccountBaseData
import com.mysql.cj.util.StringUtils
import io.vertx.core.AsyncResult
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import com.alibaba.fastjson.JSONObject
import msgScheme.MsgScheme.CreateAccountResponse.Reason
import msgScheme.MsgScheme._

case class ConnectionMsg(accountId: String, position: String, status: String, serverWebSocket: ServerWebSocket, tempPassword: Int) {


}

object ConnectionMsg {
  def genConnectMsgWithNoTempPassword(accountId: String, position: String, status: String, serverWebSocket: ServerWebSocket): ConnectionMsg = {
    ConnectionMsg(accountId, position, status, serverWebSocket, -1)
  }
}

object JDBCLib {

  case class ReadTestResult(ok: Boolean)

  def genWhereString(pairs: Seq[(String, String)]): String = {
    val s = pairs.map(aPair => {
      val where = aPair._1
      val value = aPair._2
      where + "=\"" + value + "\"" + " AND "
    }).reduce(_ + _)
    s.substring(0, s.length - 4)
  }


  def readSqlRow(JDBCClient: JDBCClient, wheres: String, values: String, table: String): ReadTestResult
  = {
    var readTestResult = ReadTestResult(false)
    val sql = "SELECT * FROM " + table + " WHERE " + wheres + "=\"" + values + "\""

    JDBCClient.query(sql, res => {
      if (res.succeeded()) {
        println("result:" + res.result().getRows)
        readTestResult = ReadTestResult(true)
      }
      else {
        println("read fail:" + res.cause().getMessage)
      }
    })
    readTestResult
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
        i += 1;
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

  def accountCheck(JDBCClient: JDBCClient, account: String, password: String): AccountBaseData = {
    val passwordInTable = getSha1(password)
    val sql = "SELECT * WHERE accountId=" + account + " FROM " + SqlConfig.accountBaseTable
    var ok = false
    var reasion = ""

    JDBCClient.query(sql, res => {
      if (res.succeeded()) {
        val resultSet = res.result()
        println(resultSet)
      }
    })
    AccountBaseData("", "", "", "", -1)
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

  def accountCreate(jc: JDBCClient, abd: AccountBaseData): CreateRes = {


    var result: CreateRes = if (abd.password.forall(_.isLetterOrDigit)) CreateRes(false, Reason.NO_GOOD_PASSWORD) else CreateRes(false, Reason.OTHER)

    val sql = "INSERT INTO " + SqlConfig.accountBaseTable + SqlConfig.accountBaseInsertScheme + abd.genValue + ";"
    jc.query(sql, res => {
      if (res.succeeded()) {
        result = CreateRes(true, Reason.OK)
        println("create account ok")
      }
      else {
        result = CreateRes(false, Reason.ALREADY_EXIST)
        println("create account fail:" + res.cause().getMessage)
      }
    })
    result
  }

  def tableCheckAndCreate(jc: JDBCClient, tableName: String): Boolean = {
    val sql = "DESCRIBE " + tableName
    jc.query(sql, (qryRes: AsyncResult[ResultSet]) => {
      if (qryRes.succeeded) {
        val resultSet = qryRes.result
        println("describe res:" + resultSet.getRows)
      }
      else {
        println("TableNotFound:" + tableName)
        val cSql = SqlConfig.schemaMap(tableName)
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
    true
  }

  def tableCheckAllAndCreate(JDBCClient: JDBCClient): Boolean = {

    SqlConfig.schemaMap.keys.forall(t =>
      tableCheckAndCreate(JDBCClient, t))

  }


}
