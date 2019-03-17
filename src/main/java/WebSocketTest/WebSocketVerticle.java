package WebSocketTest;


import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

import io.vertx.ext.web.Router;

import org.javatuples.Triplet;


import java.util.HashMap;
import java.util.Map;

public class WebSocketVerticle extends AbstractVerticle {
    // 保存每一个连接到服务器的通道
    private Map<String, Triplet<String, String, ServerWebSocket>> connectionMap = new HashMap<>(16);

    /*
    元组为：玩家位置，玩家状态，玩家webSocket
    位置：loginHall 去大厅
            inHall  在大厅
            goRoom  正在去房间
            Room + 数字  在房间，数字代表房间号
     状态：free 未在游戏中
            play 在游戏中

     * */
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
                connectionMap.put(id, new Triplet<>("loginHall", "free", webSocket));
                System.out.print("ws：id列表");
                for (Map.Entry<String, Triplet<String, String, ServerWebSocket>> entry : connectionMap.entrySet()) {
                    System.out.println(entry.getKey());
                }
                System.out.println("ws：发送进入大厅请求：" + id);
                eb.send("player.inHall", id, ar -> {
                            if (ar.succeeded()) {
                                String who = ar.result().body().toString();
                                System.out.println("ws:收到进入大厅回复：" + who);
                                if (who.equals(id) && connectionMap.get(who).getValue0().equals("loginHall")) {

                                    connectionMap.put(id, new Triplet<>("inHall", "free", webSocket));
                                    System.out.println("ws：进入大厅成功" + who);
                                } else {
                                    connectionMap.remove(who);
                                    System.out.println("ws：没有此玩家或玩家状态错误：" + who);
                                    webSocket.close();

                                }
                            }
                        }
                );
            } else {
                //TODO ???
            }

            //InHallResp


            eb.consumer("joinRoom", who -> {

            });

            //OFFLineResp
            webSocket.closeHandler(handler -> {
                        String playerPosition = connectionMap.get(id).getValue0();

                        if (playerPosition.startsWith("Room")) {
                            eb.send("offLine" + playerPosition, id);
                        } else
                            switch (connectionMap.get(id).getValue0()) {
                                case "loginHall":
                                    eb.send("cancelLogin", id);
                                    break;
                                case "inHall":
                                    eb.send("quitHall", id);
                                    break;
                                case "creatingRoom":
                                    eb.send("cancelCreate", id);
                                    break;


                            }
                        eb.send("playerOffLineInHall", id);
                        connectionMap.remove(id);
                    }
            );


            //　WebSocket 连接
            webSocket.frameHandler(handler -> {
                String textData = handler.textData();
                String currID = webSocket.binaryHandlerID();
                //TODO proto decode

                switch (textData) {
                    case "createRoom": {
                        if (connectionMap.get(currID).getValue0().equals("inHall")) {
                            connectionMap.put(currID, new Triplet<>("creatingRoom", "free", webSocket));
                            eb.send("createRoom", currID, ar ->

                            {
                                JSONObject crInfo =
                                        JSONObject.parseObject(ar.result().body().toString());

                                String cid = crInfo.getString("Id");
                                int RoomId = crInfo.getInteger("roomId");
                                if (connectionMap.get(cid).getValue0().equals("creatingRoom") && cid.equals(id)) {
                                    connectionMap.put(cid, new Triplet<>("Room" + RoomId, "free", webSocket));
                                    System.out.println("回复房间:" + RoomId + "开启ok" + cid);
                                    connectionMap.get(cid).getValue2().writeTextMessage("房间建立成功，房间号：" + RoomId);

                                } else {
                                    System.out.println("ws：createRoom：没有此玩家或玩家状态错误：" + cid);

                                }

                            });
                        } else {
                            connectionMap.get(currID).getValue2().writeTextMessage("不可建立房间，你不在大厅中");
                        }

                        break;
                    }
                    case "joinRoom": {
                        eb.send("joinRoom", currID);
                        connectionMap.put(currID, new Triplet<>("goRoom", "free", webSocket));
                        break;
                    }

                    case "fastPlay": {
                        eb.send("fastPlay", currID);
                        connectionMap.put(currID, new Triplet<>("goRoom", "free", webSocket));
                        break;
                    }

                    default:
                        //给非当前连接到服务器的每一个WebSocket连接发送消息
                        for (Map.Entry<String, Triplet<String, String, ServerWebSocket>> entry : connectionMap.entrySet()) {

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
                            entry.getValue().getValue2().writeTextMessage("用户" + id + ":" + textData + "\r");
                        }
                        break;
                }
            });
        });
    }
}

