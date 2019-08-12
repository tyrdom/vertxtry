package WebSocketTest

import io.vertx.core.http
import io.vertx.core.http.ServerWebSocket
import msgScheme.MsgScheme.RoomPlayerStatusBroadcast.OnePlayerInRoom


object RoomStatus extends Enumeration { //如果条件都能找到满足，那么就会触发技能效果，
type RoomStatus = Value
  val Standby: RoomStatus = Value
  val Gaming: RoomStatus = Value
  val Full: RoomStatus = Value
  val Other: RoomStatus = Value
}

object PlayerStandStatus extends Enumeration { //如果条件都能找到满足，那么就会触发技能效果，
type PlayerStandStatus = Value
  val Standby: PlayerStandStatus = Value
  val Play: PlayerStandStatus = Value
  val Ready: PlayerStandStatus = Value
  val Other: PlayerStandStatus = Value
}

object PlayerStatus {
  def getStatus(tempId: Int, aid: String): PlayerStatus = {
    val str = WebTable.onlineAccount.get(aid) match {
      case Some(value) => value.binaryHandlerID()
      case None => ""
    }
    WebTable.connectMap.get(str) match {
      case Some(value) =>
        val maybeSocket: Option[http.ServerWebSocket] = Option.apply(value.serverWebSocket)
        PlayerStatus(tempId, OnePlayerInRoom.Status.STANDBY, value.accountId, maybeSocket)
      case None => PlayerStatus.zero
    }
  }

  val zero = PlayerStatus(-1, OnePlayerInRoom.Status.OFFLINE, "", None)
}

case class PlayerStatus(tempId: Int, status: OnePlayerInRoom.Status, nickname: String, webSocketServer: Option[ServerWebSocket]) {
  def changeStatus(status: OnePlayerInRoom.Status): PlayerStatus = {
    PlayerStatus(tempId = tempId, status, this.nickname, this.webSocketServer)
  }
}