package WebSocketTest

import com.alibaba.fastjson.JSONObject
import msgScheme.MsgScheme._

object CodeMsgTranslate {
  def encode(head: String, body: JSONObject): Array[Byte] = (head, body) match {
    case ("Login_Response", somebody) =>
      val ok = somebody.getBoolean("ok")
      val bodyBuilder = LoginResponse.newBuilder().setOk(ok)
      val msgBuilder = AMsg.newBuilder().setHead(AMsg.Head.Login_Response).setLoginResponse(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
//TODO 其他的答复encode在这里加
      //test ok
    case ("Login_Request", somebody) =>
      val userId = somebody.getString("userId")
      val password =somebody.getString("password")
      val bodyBuilder = LoginRequest.newBuilder().setUserId(userId).setPassword(password)
      val msgBuilder = AMsg.newBuilder().setHead(AMsg.Head.Login_Request).setLoginRequest(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
    case (_, _) =>
      val code = AMsg.newBuilder().setHead(AMsg.Head.Undefined_Response).setUndefinedResponse(UndefinedResponse.newBuilder()).build().toByteArray
      code
  }

  def decode(byteArray: Array[Byte]): JSONObject = {
    val msg = AMsg.parseFrom(byteArray)
    val head = msg.getHead
//    println(head)
    val jsonObj = (head, msg) match {
      case (AMsg.Head.Login_Request, someMsg) =>
        val theJsonObj: JSONObject = new JSONObject()
        val uid = someMsg.getLoginRequest.getUserId
       // println(uid)
        val password = someMsg.getLoginRequest.getPassword
        theJsonObj.put("head", "Login_Request")
        theJsonObj.put("userId", uid)
        theJsonObj.put("password", password)
        theJsonObj
//TODO 其他的请求decode在这里加


      case (_, _) =>
        val theJsonObj: JSONObject = new JSONObject()
        theJsonObj.put("head", "Undefined_Request")
        theJsonObj

    }
    jsonObj
  }

}