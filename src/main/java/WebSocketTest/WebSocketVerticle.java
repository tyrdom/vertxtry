package WebSocketTest;


import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;

import msgScheme.MsgScheme;
import org.javatuples.Triplet;
import scala.Tuple2;


import java.util.HashMap;
import java.util.Map;

//WebSocket 服务为玩家消息代理，向其他服务者发出和接收消息
public class WebSocketVerticle extends AbstractVerticle {
    // 保存每一个连接到服务器的通道
    //用户Id，用户信息
    private Map<String, ConnectionMsg> connectionMap = new HashMap<>(16);

    /*
    元组为：玩家位置，玩家状态，玩家webSocket
    位置：loginHall 去大厅
            inHall  在大厅
            goRoom  正在去房间
            Room + 数字  在房间，数字代表房间号
     状态：free 未在游戏中
            play 在游戏中
            号码 正在进入房间X，但是还未正式进入
     * */
    private boolean checkID(String aid) {
        return connectionMap.containsKey(aid);
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            routingContext.response().sendFile("html/ws.html");
        });
        webSocketMethod(server);
        server.requestHandler(router).listen(8080);
        DeploymentOptions hallOptions = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(HallVerticle.class.getName(), hallOptions);
        DeploymentOptions jdbcOptions = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(JdbcVerticle.class.getName(), jdbcOptions);
    }

    private void goHallProcess(String someId, ServerWebSocket someWebSocket, EventBus eb) {
        //

        connectionMap.put(someId, new ConnectionMsg("id", "loginHall", "free", someWebSocket));

        System.out.println("ws：发送进入大厅请求：" + someId);
        eb.send("player.inHall", someId, ar -> {
                    if (ar.succeeded()) {
                        String who = ar.result().body().toString();
                        System.out.println("ws:收到进入大厅回复：" + who);
                        if (who.equals(someId) && connectionMap.get(who).position().equals("loginHall")) {

                            connectionMap.put(someId, new ConnectionMsg("unknown", "inHall", "free", someWebSocket));
                            System.out.println("ws：进入大厅成功" + who);

                        } else {
                            connectionMap.remove(who);
                            System.out.println("ws：没有此玩家或玩家状态错误：" + who);
                            someWebSocket.close();

                        }

                    } else {
                        System.out.println("进入大厅消息未回复，大厅处理有问题");
                    }
                }
        );
    }

    private void webSocketMethod(HttpServer server) {
        server.websocketHandler(webSocket -> { // 获取每一个链接的ID
            String id = webSocket.binaryHandlerID();
            //TODO Login Server
            EventBus eb = vertx.eventBus();

            // 判断当前连接的ID是否存在于map集合中，如果不存在则添加进map集合中


            if (!checkID(id)) {
                //TODO connecting
                goHallProcess(id, webSocket, eb);
            } else {
                //TODO reconnect
            }
            System.out.print("ws：id列表");
            for (Map.Entry<String, ConnectionMsg> entry : connectionMap.entrySet()) {
                System.out.println(entry.getKey());
            }


            //OFFLineResp
            webSocket.closeHandler(handler -> {
                        String playerPosition = connectionMap.get(id).position();
                        JSONObject whoAndReason = new JSONObject();
                        System.out.println(id + "已断开");
                        whoAndReason.put("id", id);
                        whoAndReason.put("reason", "offLine");

                        String whoAndReasonMsg = whoAndReason.toJSONString();
                        if (playerPosition.startsWith("Room")) {
                            eb.send("quit" + playerPosition, whoAndReasonMsg);
                        } else
                            switch (connectionMap.get(id).position()) {
                                case "loginHall":
                                    eb.send("cancelLogin", whoAndReasonMsg);
                                    break;
                                //如果状态已在大厅，直接发送id
                                case "inHall":
                                    eb.send("quitHall", id);
                                    break;
                                case "creatingRoom":
                                    eb.send("cancelCreate", whoAndReasonMsg);
                                    break;

                                case "findingRoom":
                                    //向大厅发送取消寻找，如果不是早于大厅的寻找流程，那么就会向房间发送退出消息
                                    eb.send("cancelFind", whoAndReasonMsg,
                                            messageAsyncResult -> {
                                                if (messageAsyncResult.succeeded())
                                                    if (!messageAsyncResult.result().body().equals("early")) {
                                                        String roomId = messageAsyncResult.result().body().toString();
                                                        eb.send("quitRoom" + roomId, whoAndReasonMsg);
                                                    }
                                            }

                                    );
                                    break;
                                //在正在加入状态，说明在状态上保存了房间号，但是没有进入，通知该房间谁退出
                                case "joiningRoom":
                                    String RoomId = connectionMap.get(id).status();
                                    eb.send("quitRoom" + RoomId, whoAndReasonMsg);
                                    break;

                            }


                        connectionMap.remove(id);
                    }
            );


            //　WebSocket 连接
            webSocket.frameHandler(handler -> {
//                String textData = handler.textData();
                String currID = webSocket.binaryHandlerID();
                try {
                    byte[] binData = handler.binaryData().getBytes();
                    Tuple2<MsgScheme.AMsg.Head, JSONObject> msg = CodeMsgTranslate.decode(binData);

                    MsgScheme.AMsg.Head head = msg._1;
                    JSONObject body = msg._2;
                    ServerWebSocket responseWebSocket = connectionMap.get(currID).serverWebSocket();
                    switch (head) {
                        case Login_Request:
                            String userId = body.getString("userId");
                            String password = body.getString("password");
                            System.out.println("收到登录消息" + "userId:" + userId + "===password:" + password);
                            eb.send("loginGame",body.toJSONString(),messageAsyncResult -> {});
                            //TODO 登陆处理

                            JSONObject responseBody = new JSONObject();
                            responseBody.put("ok", true);
                            System.out.println("js:" + responseBody);
                            byte[] loginBin = CodeMsgTranslate.encode(MsgScheme.AMsg.Head.Login_Response, responseBody);

                            responseWebSocket.writeBinaryMessage(Buffer.buffer(loginBin));
                            break;
                        case CreateRoom_Request:
                            if (connectionMap.get(currID).position().equals("inHall")) {
                                {
                                    connectionMap.put(currID, new ConnectionMsg("", "creatingRoom", "free", webSocket));
                                    eb.send("createRoom", currID, ar ->

                                    {
                                        JSONObject crInfo =
                                                JSONObject.parseObject(ar.result().body().toString());

                                        String cid = crInfo.getString("id");
                                        int RoomId = crInfo.getInteger("room");
                                        if (connectionMap.get(cid).position().equals("creatingRoom") && cid.equals(id)) {
                                            connectionMap.put(cid, new ConnectionMsg("", "Room" + RoomId, "free", webSocket));
                                            System.out.println("回复房间:" + RoomId + "开启ok" + cid);
                                            byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.CreateRoom_Response).setCreateRoomResponse(MsgScheme.CreateRoomResponse.newBuilder().setRoomId(RoomId)).build().toByteArray();

                                            responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));

                                        } else {
                                            System.out.println("ws：createRoom：没有此玩家或玩家状态错误：" + cid);
                                            byte[] toSend = CodeMsgTranslate.genErrorBytes("没有此玩家或玩家状态错误");

                                            responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                        }
                                    });
                                }
                            } else {
                                byte[] toSend = CodeMsgTranslate.genErrorBytes("不可建立房间，你不在大厅中");
                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));

                            }
                            break;

                        case JoinRoom_Request: {
                            if (connectionMap.get(currID).position().equals("inHall")) {
                                connectionMap.put(currID, new ConnectionMsg("sb", "findingRoom", "free", webSocket));
                                //向大厅请求一个有位置的房间号
                                eb.send("findRoom", currID, messageAsyncResult -> {
                                    if (messageAsyncResult.succeeded() && !messageAsyncResult.result().body().equals("fail")) {

                                        String roomId = messageAsyncResult.result().body().toString();
                                        //请求到房间成功后，开始进入房间
                                        connectionMap.put(currID, new ConnectionMsg("", "joiningRoom", roomId, webSocket));
                                        eb.send("joinRoom" + roomId, currID, messageAsyncResult1 -> {
                                            //房间回复ok，则记录在房间的状态
                                            if (messageAsyncResult1.succeeded() && messageAsyncResult1.result().body().equals("ok")) {
                                                byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.JoinRoom_Response).setJoinRoomResponse(MsgScheme.JoinRoomResponse.newBuilder().setRoomId(Integer.valueOf(roomId))).build().toByteArray();
                                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                                connectionMap.put(currID, new ConnectionMsg("", "Room" + roomId, "free", webSocket));
                                            } else {
                                                byte[] toSend = CodeMsgTranslate.genErrorBytes("房间出问题，不可进入");
                                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                                connectionMap.put(currID, new ConnectionMsg("", "inHall", "free", webSocket));
                                            }
                                        });

                                    } else {
                                        byte[] toSend = CodeMsgTranslate.genErrorBytes("没有剩余的空房间,请创建房间");
                                        responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                        connectionMap.put(currID, new ConnectionMsg("", "inHall", "free", webSocket));
                                    }
                                });
                            } else {
                                byte[] toSend = CodeMsgTranslate.genErrorBytes("不可加入房间，你不在大厅中");
                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                            }
                            break;
                        }
                        case QuitRoom_Request: {
                            if (connectionMap.get(currID).position().startsWith("Room")) {
                                JSONObject whoAndReason = new JSONObject();
                                whoAndReason.put("id", currID);
                                whoAndReason.put("reason", "normal");
                                String whoAndReasonMsg = whoAndReason.toJSONString();
                                eb.send("quit" + connectionMap.get(currID).position(), whoAndReasonMsg);
                                goHallProcess(currID, webSocket, eb);

                                byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.QuitRoom_Response).setQuitRoomResponse(MsgScheme.QuitRoomResponse.newBuilder()).build().toByteArray();
                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                            } else {

                                byte[] toSend = CodeMsgTranslate.genErrorBytes("不可退出房间，你不在房间中");
                                responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                            }
                        }
                        break;

                        default:
                            byte[] toSend = CodeMsgTranslate.genErrorBytes("无效消息，连接关闭");
                            responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                            responseWebSocket.close();
                    }

                } catch (Exception e) {

                }

                //创建 加入房间只能位于大厅操作 字符串协议
//                switch (textData) {
//                    case "createRoom": {
//                        if (connectionMap.get(currID).getValue0().equals("inHall")) {
//                            {
//                                connectionMap.put(currID, new Triplet<>("creatingRoom", "free", webSocket));
//                                eb.send("createRoom", currID, ar ->
//
//                                {
//                                    JSONObject crInfo =
//                                            JSONObject.parseObject(ar.result().body().toString());
//
//                                    String cid = crInfo.getString("id");
//                                    int RoomId = crInfo.getInteger("room");
//                                    if (connectionMap.get(cid).getValue0().equals("creatingRoom") && cid.equals(id)) {
//                                        connectionMap.put(cid, new Triplet<>("Room" + RoomId, "free", webSocket));
//                                        System.out.println("回复房间:" + RoomId + "开启ok" + cid);
//                                        connectionMap.get(cid).getValue2().writeTextMessage("房间建立成功，房间号：" + RoomId);
//
//                                    } else {
//                                        System.out.println("ws：createRoom：没有此玩家或玩家状态错误：" + cid);
//                                    }
//                                });
//                            }
//                        } else {
//                            //TODO protobuf
//                            connectionMap.get(currID).getValue2().writeTextMessage("不可建立房间，你不在大厅中");
//                        }
//                        break;
//                    }
//                    case "joinRoom": {
//                        if (connectionMap.get(currID).getValue0().equals("inHall")) {
//                            connectionMap.put(currID, new Triplet<>("findingRoom", "free", webSocket));
//                            //向大厅请求一个有位置的房间号
//                            eb.send("findRoom", currID, messageAsyncResult -> {
//                                if (messageAsyncResult.succeeded() && !messageAsyncResult.result().body().equals("fail")) {
//                                    String roomId = messageAsyncResult.result().body().toString();
//                                    //请求到房间成功后，开始进入房间
//                                    connectionMap.put(currID, new Triplet<>("joiningRoom", roomId, webSocket));
//                                    eb.send("joinRoom" + roomId, currID, messageAsyncResult1 -> {
//                                        //房间回复ok，则记录在房间的状态
//                                        if (messageAsyncResult1.succeeded() && messageAsyncResult1.result().body().equals("ok")) {
//                                            //TODO protobuf
//                                            connectionMap.get(currID).getValue2().writeTextMessage("房间进入成功，你在房间" + roomId);
//                                            connectionMap.put(currID, new Triplet<>("Room" + roomId, "free", webSocket));
//                                        } else {
//                                            //TODO protobuf
//                                            connectionMap.get(currID).getValue2().writeTextMessage("房间出问题，不可进入");
//                                            connectionMap.put(currID, new Triplet<>("inHall", "free", webSocket));
//                                        }
//                                    });
//
//                                } else {
//                                    //TODO protobuf
//                                    connectionMap.get(currID).getValue2().writeTextMessage("没有剩余的空房间");
//                                    connectionMap.put(currID, new Triplet<>("inHall", "free", webSocket));
//                                }
//                            });
//                        } else {
//                            //TODO protobuf
//                            connectionMap.get(currID).getValue2().writeTextMessage("不可加入房间，你不在大厅中");
//                        }
//                        break;
//                    }
//                    case "leaveRoom": {
//                        if (connectionMap.get(currID).getValue0().startsWith("Room")) {
//                            JSONObject whoAndReason = new JSONObject();
//                            whoAndReason.put("id", currID);
//                            whoAndReason.put("reason", "normal");
//                            String whoAndReasonMsg = whoAndReason.toJSONString();
//                            eb.send("quit" + connectionMap.get(currID).getValue0(), whoAndReasonMsg);
//                            loginHallProcess(currID, webSocket, eb);
//                            //TODO protobuf
//                            connectionMap.get(currID).getValue2().writeTextMessage("退出成功，你回到大厅");
//                        } else {
//                            //TODO protobuf
//                            connectionMap.get(currID).getValue2().writeTextMessage("不可退出房间，你不在房间中");
//                        }
//                    }
//                    break;
//
//                    case "ready": {
//                        if (connectionMap.get(currID).getValue0().startsWith("Room") && connectionMap.get(currID).getValue1().equals("free")) {
//                            eb.send("ready" + connectionMap.get(currID).getValue0(), currID, messageAsyncResult -> {
//                                Triplet<String, String, ServerWebSocket> oldStatus = connectionMap.get(currID);
//
//                                ServerWebSocket webSocketToSend = oldStatus.getValue2();
//                                if (messageAsyncResult.succeeded()) {
//                                    String replyMsg = messageAsyncResult.result().body().toString();
//
//                                    switch (replyMsg) {
//                                        case "gameStart":
//                                            webSocketToSend.writeTextMessage("所有玩家准备完成，开始");
//                                            connectionMap.put(currID, new Triplet<>(oldStatus.getValue0(), "play", webSocketToSend));
//                                            break;
//                                        case "readyOk": {
//                                            webSocketToSend.writeTextMessage("准备完成，等待其他玩家准备");
//                                            connectionMap.put(currID, new Triplet<>(oldStatus.getValue0(), "ready", webSocketToSend));
//                                            break;
//                                        }
//                                        case "notFull":
//                                            webSocketToSend.writeTextMessage("房间人数未满，暂时不可准备");
//                                            break;
//                                        default:
//                                            webSocketToSend.writeTextMessage("房间连接错误，连接断开");
//                                            webSocketToSend.close();
//                                            connectionMap.remove(currID);
//                                            break;
//                                    }
//                                } else {
//                                    webSocketToSend.writeTextMessage("超时关闭");
//                                    webSocketToSend.close();
//                                    connectionMap.remove(currID);
//                                }
//                            });
//                        } else {
//                            connectionMap.get(currID).getValue2().writeTextMessage("不可准备，你已经准备或在游戏中，或者你不在房间");
//
//                        }
//                    }
//                    break;
//
//                    default:
//
////                        //给非当前连接到服务器的每一个WebSocket连接发送消息
////                        for (Map.Entry<String, Triplet<String, String, ServerWebSocket>> entry : connectionMap.entrySet()) {
////
////                            if (currID.equals(entry.getKey())) {
////                                entry.getValue().getValue2().writeTextMessage("yourself:" + textData + "\r");
////                            }
////
////                    /* 发送文本消息 文本信息似乎不支持图片等二进制消息
////                    若要发送二进制消息，可用writeBinaryMessage方法
////                    */
//////
//////                            JSONObject msg = new JSONObject();
//////                            msg.put("name", "abc");
//////                            msg.put("age", 11);
//////                            String output = msg.toJSONString();
//////                            System.out.println(output);
//////                            eb.send("test.address", output);
////                            else {
////                                entry.getValue().getValue2().writeTextMessage("用户" + id + ":" + textData + "\r");
////                            }
////                        }
////                        break;
//                }
            });
        });
    }
}

