package WebSocketTest

import io.vertx.core.http.ServerWebSocket

object WebTable {
  var onlineAccount: Map[String, ServerWebSocket] = Map[String, ServerWebSocket]()

  def onlineAccountAdd(accountId: String, webSocket: ServerWebSocket): Unit =
    onlineAccount += accountId -> webSocket

  def onlineAccountRemove(accountId: String): Unit =
    onlineAccount -= accountId

  var connectMap: Map[String, ConnectionMsg] = Map[String, ConnectionMsg]()

  def connectMapAdd(accountId: String, connectMsg: ConnectionMsg): Unit =
    connectMap += accountId -> connectMsg

  def connectMapRemove(accountId: String): Unit =
    connectMap -= accountId

  def getSocketBin(option: Option[ServerWebSocket]): String = option match {
    case Some(value) => value.binaryHandlerID()
    case None => ""
  }

}

