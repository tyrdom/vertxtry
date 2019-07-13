package WebSocketTest

import com.alibaba.fastjson.JSONObject
import msgScheme.MsgScheme.AMsg.Head
import msgScheme.MsgScheme._

//由于vert.x常用JSONString传送数据，所以把body 以JSON格式encode decode
object CodeMsgTranslate {

  def genErrorBytes(reason: String): Array[Byte] = AMsg.newBuilder().setHead(Head.Error_Response).setErrorResponse(ErrorResponse.newBuilder().setReason(reason)).build().toByteArray


  def encode(head: Head, body: JSONObject): Array[Byte] = (head, body) match {
    case (Head.Login_Response, somebody) =>
      val ok = somebody.getBoolean("ok")
      val reason = somebody.getString("reason")
      println("encode:" + ok)
      val bodyBuilder = LoginResponse.newBuilder().setOk(ok).setReason(reason)
      val msgBuilder = AMsg.newBuilder().setHead(head).setLoginResponse(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
    //TODO 其他的答复encode在这里加
    //test用 ok
    case (Head.Login_Request, somebody) =>
      val userId = somebody.getString("userId")
      val password = somebody.getString("password")
      val bodyBuilder = LoginRequest.newBuilder().setUserId(userId).setPassword(password)
      val msgBuilder = AMsg.newBuilder().setHead(head).setLoginRequest(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code
    //      message CreateRoomResponse {
    //
    //        int32 roomId = 1;
    //      }
    case (Head.CreateRoom_Response, sb) =>

      val roomId = sb.getInteger("roomId")
      val bodyBuilder = CreateRoomResponse.newBuilder().setRoomId(roomId)
      val msgBuilder = AMsg.newBuilder().setHead(head).setCreateRoomResponse(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code

    //        message JoinRoomResponse {
    //
    //          int32 roomId = 1;
    //        }
    case (Head.JoinRoom_Response, sb) =>
      val roomId = sb.getInteger("roomId")
      val bodyBuilder = JoinRoomResponse.newBuilder().setRoomId(roomId)
      val msgBuilder = AMsg.newBuilder().setHead(head).setJoinRoomResponse(bodyBuilder)
      val code = msgBuilder.build().toByteArray
      code

    case (_, _) =>
      val code = AMsg.newBuilder().setHead(AMsg.Head.Undefined_Response).setUndefinedResponse(UndefinedResponse.newBuilder()).build().toByteArray
      code
  }

  def decode(byteArray: Array[Byte]): (Head, JSONObject) = {
    val msg = AMsg.parseFrom(byteArray)
    val head = msg.getHead
    //    println(head)
    val jsonObj = (head, msg) match {
      case (AMsg.Head.Login_Request, someMsg) =>
        val theJsonObj: JSONObject = new JSONObject()
        val uid = someMsg.getLoginRequest.getUserId
        // println(uid)
        val password = someMsg.getLoginRequest.getPassword

        theJsonObj.put("accountId", uid)
        theJsonObj.put("password", password)
        (head, theJsonObj)
      //TODO 其他的请求decode在这里加

      //      case class Login_Request(head:Head,userId:String,password: String)
      //      Login_Request(AMsg.Head.Login_Request,uid,password)
      case (Head.Login_Response, someBody) =>
        val theJSBody = new JSONObject()
        val ok = someBody.getLoginResponse.getOk

        theJSBody.put("ok", ok)
        (head, theJSBody)

      //        message CreateRoomRequest {
      //        }

      case (Head.CreateRoom_Request, _) =>
        val theJSBody = new JSONObject()
        (head, theJSBody)
      //
      //          message JoinRoomRequest {
      //            bool certainRoom = 1;
      //            int32 roomNum = 2;
      //          }
      case (Head.JoinRoom_Request, sb) =>
        val theJSBody = new JSONObject()
        val certainRoom = sb.getJoinRoomRequest.getCertainRoom
        val roomId = sb.getJoinRoomRequest.getRoomId
        theJSBody.put("certainRoom", certainRoom)
        theJSBody.put("roomId", roomId)
        (head, theJSBody)


      case (_, _) =>
        val theJsonObj: JSONObject = new JSONObject()
        theJsonObj.put("head", "Undefined_Request")
        (Head.Undefined_Request, theJsonObj)

    }
    jsonObj
  }

}