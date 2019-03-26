package WebSocketTest

import com.alibaba.fastjson.JSONObject
import msgScheme.MsgScheme.AMsg.Head
import msgScheme.MsgScheme._
//由于vert.x常用JSONString传送数据，所以把body 以JSON格式encode decode
object CodeMsgTranslate {
  def encode(head: Head, body: JSONObject): Array[Byte] = (head, body) match {
    case (Head.Login_Response, somebody) =>
      val ok = somebody.getBoolean("ok")
      val bodyBuilder = LoginResponse.newBuilder().setOk(ok)
      val msgBuilder = AMsg.newBuilder().setHead(head).setLoginResponse(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
//TODO 其他的答复encode在这里加
      //test ok
    case (Head.Login_Request, somebody) =>
      val userId = somebody.getString("userId")
      val password =somebody.getString("password")
      val bodyBuilder = LoginRequest.newBuilder().setUserId(userId).setPassword(password)
      val msgBuilder = AMsg.newBuilder().setHead(head).setLoginRequest(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
    case (_, _) =>
      val code = AMsg.newBuilder().setHead(AMsg.Head.Undefined_Response).setUndefinedResponse(UndefinedResponse.newBuilder()).build().toByteArray
      code
  }

  def decode(byteArray: Array[Byte]):(Head,JSONObject) = {
    val msg = AMsg.parseFrom(byteArray)
    val head = msg.getHead
//    println(head)
    val jsonObj = (head, msg) match {
      case (AMsg.Head.Login_Request, someMsg) =>
        val theJsonObj: JSONObject = new JSONObject()
        val uid = someMsg.getLoginRequest.getUserId
       // println(uid)
        val password = someMsg.getLoginRequest.getPassword

        theJsonObj.put("userId", uid)
        theJsonObj.put("password", password)
        (head,theJsonObj)
//TODO 其他的请求decode在这里加

//      case class Login_Request(head:Head,userId:String,password: String)
//      Login_Request(AMsg.Head.Login_Request,uid,password)

      case (_, _) =>
        val theJsonObj: JSONObject = new JSONObject()
        theJsonObj.put("head", "Undefined_Request")
        theJsonObj

    }
    jsonObj
  }

}