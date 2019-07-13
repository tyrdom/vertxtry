package WebSocketTest

import java.security.MessageDigest

import com.mysql.cj.util.StringUtils
import io.vertx.core.AsyncResult
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import msgScheme.MsgScheme.LoginResponse
import sun.security.util.Password

case class ConnectionMsg(accountId: String, position: String, status: String, serverWebSocket: ServerWebSocket)

object JDBCLib {
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

  def accountCheck(JDBCClient: JDBCClient, account: String, password: String): (Boolean, String) = {
    val passwordInTable = getSha1(password)
    val sql = "SELECT * WHERE accountId=" + account + "FROM" + SqlConfig.accountBase
    (true, "ok")
  }

  def tableCheckAndCreate(jc: JDBCClient, tableName: String): Unit = {
    val sql = "DESCRIBE " + tableName
    jc.query(sql, (qryRes: AsyncResult[ResultSet]) => {
      if (qryRes.succeeded) {
        val resultSet = qryRes.result
        println(resultSet)
      }
      else {
        println("TableNotFound:" + tableName)
        val cSql = SqlConfig.schemaMap(tableName)

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

  def tableCheckAllAndCreate(JDBCClient: JDBCClient) = {
    SqlConfig.schemaMap.keys.foreach(t =>
      tableCheckAndCreate(JDBCClient, t))
  }


}
