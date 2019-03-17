package WebSocketTest;


import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

import io.vertx.ext.web.Router;
import javafx.util.Pair;
import scala.Tuple2;


import java.util.HashMap;
import java.util.Map;

public class WebSocketVerticle extends AbstractVerticle {
    // 保存每一个连接到服务器的通道
    private Map<String, Pair<String, ServerWebSocket>> connectionMap = new HashMap<>(16);

    private boolean checkID(String aid) {
        return connectionMap.containsKey(aid);
    }

    @Override
    public void start() throws Exception {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            routingContext.response().sendFile("html/ws.html");
        });
        webSocketMethod(server);
        server.requestHandler(router).listen(8080);
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(HallVerticle.class.getName(), options);
    }

    public void webSocketMethod(HttpServer server) {
        server.websocketHandler(webSocket -> { // 获取每一个链接的ID
            String id = webSocket.binaryHandlerID();
            //TODO Login Server
            EventBus eb = vertx.eventBus();
            // 判断当前连接的ID是否存在于map集合中，如果不存在则添加进map集合中
            if (!checkID(id)) {
                //TODO reconnect
                connectionMap.put(id, new Pair<>("loginHall", webSocket));

                eb.send("player.inHall", id);
            } else {
                //TODO ???
            }

            //InHallResp
            eb.consumer("player.inHall", msg -> {
                if (msg.body().equals("ok")) {

                    connectionMap.put(id, new Pair<>("inHall", webSocket));
                } else {
                    connectionMap.remove(id);
                    webSocket.close();

                }
            });

            eb.consumer("joinRoom", who -> {

            });

            //OFFLineResp
            webSocket.closeHandler(handler -> {
                        eb.send("playerOffLine", id);
                        connectionMap.remove(id);
                    }
            );

            eb.consumer("playerOffLine",msg-> msg.body());

            //CreateRoomResp
            eb.consumer("createRoom", msg -> {
                JSONObject crInfo =
                        JSONObject.parseObject(msg.body().toString());

                String cid = crInfo.getString("Id");
                int RoomId = crInfo.getInteger("roomId");
                if (connectionMap.containsKey(cid) && cid.equals(id)) {
                    connectionMap.put(cid, new Pair<>("inRoom" + RoomId, webSocket));
                    connectionMap.get(cid).getValue().writeTextMessage("createRoomOK");
                } else {
                    System.out.println("not such user connect" + cid);

                }
            });


            //　WebSocket 连接
            webSocket.frameHandler(handler -> {
                String textData = handler.textData();
                String currID = webSocket.binaryHandlerID();
                //TODO proto decode
                if (textData.equals("createRoom")) {
                    eb.send("createRoom", currID);

                } else if (textData.equals("joinRoom")) {
                    eb.send("joinRoom", currID);
                } else if (textData.equals("fastPlay")) {
                    eb.send("fastPlay", currID);
                }



                //给非当前连接到服务器的每一个WebSocket连接发送消息
                for (Map.Entry<String, Pair<String, ServerWebSocket>> entry : connectionMap.entrySet()) {

                    if (currID.equals(entry.getKey())) {
                        continue;
                    }

                    /* 发送文本消息 文本信息似乎不支持图片等二进制消息
                    若要发送二进制消息，可用writeBinaryMessage方法
                    */

                    JSONObject msg = new JSONObject();
                    msg.put("name", "abc");
                    msg.put("age", 11);
                    String output = msg.toJSONString();
                    System.out.println(output);
                    eb.send("test.address", output);
                    entry.getValue().getValue().writeTextMessage("用户" + id + ":" + textData + "\r");
                }
            });
        });
    }
}
