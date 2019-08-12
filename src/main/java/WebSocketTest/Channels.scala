package WebSocketTest

object Channels {
  val createAccount = "createAccount"

  val loginGame = "loginGame"

  val offline = "offline"
  val createRoom = "createRoom"
  val findRoom = "findRoom"
  val joinRoomNum = "joinRoom"
  val readyRoom = "readyRoom"
  val quitRoom = "quit"
  val cancelHallIn = "cancelLogin"

  val leftRoom = "leftRoom"
  val haveInRoom = "haveInRoom"
  val quitHall = "quitHall"
  val cancelCreate = "cancelCreate"
  val cancelFind = "cancelFind"
  val playerInHall = "player.inHall"
  val roomBroadcast = "room_broadcast"
}
