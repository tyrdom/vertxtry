

import WebSocketTest.CodeMsgTranslate;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import gameplayLib.Card;
import msgScheme.MsgScheme;
import org.junit.Test;
import scala.Tuple2;

public class ProtoTest {
    @Test
    public void testN() throws InvalidProtocolBufferException {
//        PersonModel.Person.Builder builder = PersonModel.Person.newBuilder();
//        builder.setId(1);
//        builder.setName("jihite");
//        builder.setEmail("jihite@jihite.com");
//
//        PersonModel.Person person = builder.build();
//        System.out.println("before:" + person);
//
//        System.out.println("===Person Byte:");
//        for (byte b : person.toByteArray()) {
//            System.out.print(b);
//        }

//        //encode
//        JSONObject loginRequestBody = new JSONObject();
//        loginRequestBody.put("userId","someId");
//        loginRequestBody.put("password","somePassword");
//        byte[] someByteArray =CodeMsgTranslate.encode(MsgScheme.AMsg.Head.Login_Request,loginRequestBody);
//        System.out.println("===Request Byte:");
//        for (byte b : someByteArray) {
//            System.out.print(b);
//        }



//        byte[] byteArray = person.toByteArray();
//        PersonModel.Person p2 = PersonModel.Person.parseFrom(byteArray);
//        System.out.println("after id:" + p2.getId());
//        System.out.println("after name:" + p2.getName());
//        System.out.println("after email:" + p2.getEmail());
        System.out.println(gameplayLib.Config.test());

        //decode
//        Tuple2<MsgScheme.AMsg.Head,JSONObject> decodeRequest = CodeMsgTranslate.decode(someByteArray);
//
//        System.out.println("after id:" + decodeRequest._2.getString("userId"));
//        System.out.println("after password:" + decodeRequest._2.getString("password"));
    }
}