package WebSocketTest;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

import io.vertx.ext.web.Router;
import msgScheme.MsgScheme;
import scala.None;
import scala.None$;
import scala.Option;
import scala.Tuple2;
import scala.collection.Iterable;

import java.util.HashMap;
import java.util.Map;

//WebSocket 服务为玩家消息代理，向其他服务者发出和接收消息
public class WebSocketVerticle extends AbstractVerticle {
    // 保存每一个连接到服务器的通道
    //用户Id，用户信息
//    private Map<String, ConnectionMsg> connectionMap = new HashMap<>(16);
    //    private Map<String, ServerWebSocket> onlineAccountMap = new HashMap<>(16);
    private Map<String, String> reconnectMap = new HashMap<>(16);

    /*
    为：玩家位置，玩家状态，玩家webSocket
    位置： loginSession 未登录
            loginHall 去大厅
            inHall  在大厅
            goRoom  正在去房间
            Room + 数字  在房间，数字代表房间号
     状态：free 未在游戏中
            play 在游戏中
            号码 正在进入房间X，但是还未正式进入
     * */
    private boolean checksocketId(String cid) {
        return WebTable.connectMap().contains(cid);
    }

    private boolean checkOnline(String socketId) {
        Option<ConnectionMsg> connectionMsgOption = WebTable.connectMap().get(socketId);
        String accountId = ConnectionMsg.getAccountId(connectionMsgOption);
        return WebTable.onlineAccount().contains(accountId);

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

    private void goHallProcess(String connectId, ServerWebSocket someWebSocket, EventBus eb) {

        if (checksocketId(connectId)) {
            ConnectionMsg connectionMsg = WebTable.connectMap().get(connectId).get();
            WebTable.connectMapAdd(connectId, connectionMsg.ChangePosition("loginHall").ChangeStatus("free"));
            String accountId = connectionMsg.accountId();
            System.out.println("ws：发送进入大厅请求：" + connectId);
            eb.send(Channels.playerInHall(), accountId, ar -> {
                        if (ar.succeeded()) {
                            String who = ar.result().body().toString();
                            System.out.println("ws:收到进入大厅回复：" + who);
                            Option<ServerWebSocket> socketOption = WebTable.onlineAccount().get(who);
                            String webId = WebTable.getSocketBin(socketOption);
                            if (!webId.equals("") && who.equals(accountId) && WebTable.connectMap().get(webId).get().position().equals("loginHall")) {

                                WebTable.connectMapAdd(connectId, connectionMsg.ChangeStatus("free").ChangePosition("inHall"));
                                System.out.println("ws：进入大厅成功" + who);

                            } else {

                                System.out.println("ws：没有此玩家或玩家状态错误：" + who);
                                someWebSocket.close();

                            }

                        } else {
                            System.out.println("进入大厅消息未回复，大厅处理有问题");
                        }
                    }
            );
        }
    }

    private void webSocketMethod(HttpServer server) {
        EventBus eb = vertx.eventBus();
        eb.consumer(Channels.roomBroadcast(), msg -> {
            String s = msg.body().toString();

        });

        server.websocketHandler(webSocket -> { // 获取每一个链接的ID
            String socketId = webSocket.binaryHandlerID();

            // 判断当前连接的ID是否存在于map集合中，如果不存在则添加进map集合中

            if (!checksocketId(socketId)) {

                WebTable.connectMapAdd(socketId, ConnectionMsg.genConnectMsgWithNoTempPassword("", "loginSession", "notLogin", webSocket));

            } else {
                // SocketIdError
                webSocket.close();
            }
            System.out.print("ws：id列表");
            Iterable<String> keys = WebTable.connectMap().keys();
            System.out.println(keys);

            //OFFLineResp
            webSocket.closeHandler(handler -> {
                        ConnectionMsg connectionMsg = WebTable.connectMap().get(socketId).get();
                        String playerPosition = connectionMsg.position();
                        JSONObject whoAndReason = new JSONObject();
                        String aid = connectionMsg.accountId();
                        String position = connectionMsg.position();
                        whoAndReason.put("id", aid);
                        whoAndReason.put("reason", "offLine");
                        String whoAndReasonMsg = whoAndReason.toJSONString();
                        System.out.println(socketId + "已断开:" + aid);
                        if (playerPosition.startsWith("Room")) {
                            if (connectionMsg.status().equals("play")) {


                                reconnectMap.put(aid, position);
                            } else {
                                eb.send(Channels.quitRoom() + position, whoAndReasonMsg, messageAsyncResult ->
                                {
                                    System.out.println(aid + "断线，发出退出到房间" + position);
                                    if (messageAsyncResult.succeeded()) {
                                        String s = messageAsyncResult.result().body().toString();
                                        if (s.equals("play")) {
                                            reconnectMap.put(aid, position);
                                            System.out.println("记录状态可能出错，实际为游戏中");
                                        } else System.out.println(aid + ":不在游戏中，直接退出房间:" + position);
                                    } else System.out.println("房间错误，需要检查");
                                });

                            }
                        } else
                            switch (connectionMsg.position()) {
                                case "loginHall":
                                    eb.send(Channels.cancelHallIn(), whoAndReasonMsg);
                                    break;
                                //如果状态已在大厅，直接发送id
                                case "inHall":
                                    eb.send(Channels.quitHall(), aid);
                                    break;
                                case "creatingRoom":
                                    eb.send(Channels.cancelCreate(), whoAndReasonMsg);
                                    break;

                                case "findingRoom":
                                    //向大厅发送取消寻找，如果不是早于大厅的寻找流程，那么就会向房间发送退出消息
                                    eb.send(Channels.cancelFind(), whoAndReasonMsg,
                                            messageAsyncResult -> {
                                                if (messageAsyncResult.succeeded())
                                                    if (!messageAsyncResult.result().body().equals("early")) {
                                                        String roomId = messageAsyncResult.result().body().toString();
                                                        eb.send(Channels.quitRoom() + "Room" + roomId, whoAndReasonMsg);
                                                    }
                                            }

                                    );
                                    break;
                                //在正在加入状态，说明在状态上保存了房间号，但是没有进入，通知该房间谁退出
                                case "joiningRoom":
                                    String RoomId = connectionMsg.status();
                                    eb.send(Channels.quitRoom() + "Room" + RoomId, whoAndReasonMsg);
                                    break;
                                default:
                                    eb.send(Channels.offline(), aid);
                            }
                        WebTable.connectMapRemove(socketId);
                        if (WebTable.getSocketBin(WebTable.onlineAccount().get(aid)).equals(socketId))
                            WebTable.onlineAccountRemove(aid);

                        System.out.println("ws--close------------" + WebTable.connectMap());
                        System.out.println("ws--close------------" + WebTable.onlineAccount());
                    }
            );


            //　WebSocket 连接
            webSocket.frameHandler(handler -> {
//                String textData = handler.textData();

                String connectEnsureID = webSocket.binaryHandlerID();
                boolean b = checkOnline(connectEnsureID);
                try {
                    ConnectionMsg connectionMsg = WebTable.connectMap().get(connectEnsureID).get();
                    String mapAccountId = connectionMsg.accountId();
                    System.out.println("收到请求，其id为：：：：：" + mapAccountId);
                    byte[] binData = handler.binaryData().getBytes();
                    Tuple2<MsgScheme.AMsg.Head, JSONObject> msg = CodeMsgTranslate.decode(binData);

                    MsgScheme.AMsg.Head head = msg._1;
                    JSONObject body = msg._2;
                    ServerWebSocket responseWebSocket = WebTable.connectMap().get(connectEnsureID).get().serverWebSocket();
                    switch (head) {
                        case Test_Request:
                            String testText = body.getString("testText");
                            String s = "response :" + testText;
                            System.out.println(s);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("testText", s);
                            byte[] encode = CodeMsgTranslate.encode(MsgScheme.AMsg.Head.Test_Response, jsonObject);
                            responseWebSocket.writeBinaryMessage((Buffer.buffer(encode)));
                            break;
                        case CreateAccount_Request:
                            eb.send(Channels.createAccount(), body.toJSONString(), messageAsyncResult -> {
                                if (messageAsyncResult.succeeded()) {
                                    String s1 = messageAsyncResult.result().body().toString();
                                    System.out.println("=====================================");
                                    System.out.println(s1);
                                    JSONObject respBody = JSONObject.parseObject(messageAsyncResult.result().body().toString());
                                    byte[] createAccountRespBin = CodeMsgTranslate.encode(MsgScheme.AMsg.Head.CreateAccount_Response, respBody);
                                    responseWebSocket.writeBinaryMessage((Buffer.buffer(createAccountRespBin)));
                                } else
                                    webSocket.writeBinaryMessage(Buffer.buffer(CodeMsgTranslate.genErrorBytes("账号服务异常")));
                            });
                            break;
                        case Login_Request:
                            String accountId = body.getString("accountId");
                            String password = body.getString("password");
                            System.out.println("收到登录消息" + "accountId:" + accountId + "===password:" + password);
                            eb.send(Channels.loginGame(), body.toJSONString(), messageAsyncResult -> {
                                if (messageAsyncResult.succeeded()) {
                                    JSONObject resBody = JSONObject.parseObject(messageAsyncResult.result().body().toString());
                                    String reason = resBody.getString("reason");
                                    String nickname = resBody.getString("nickname");
                                    if (reason.equals(MsgScheme.LoginResponse.Reason.OK.toString())) {

                                        System.out.println("登陆成功，记录账号跟踪，进入大厅");
                                        if (WebTable.connectMap().contains(connectEnsureID)) {

                                            if (reconnectMap.containsKey(accountId)) {
                                                String position = reconnectMap.get(accountId);
                                                WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition(position).ChangeStatus("play"));
                                            } else if (WebTable.onlineAccount().contains(accountId)) {
                                                ServerWebSocket oldWebSocket = WebTable.onlineAccount().get(accountId).get();
                                                byte[] bytes = CodeMsgTranslate.genErrorBytes(MsgScheme.ErrorResponse.ErrorType.OTHER_LOGIN, "账号重复登陆，顶掉");
                                                oldWebSocket.writeBinaryMessage(Buffer.buffer(bytes));
                                                System.out.println("login reset------------" + WebTable.connectMap());
                                                System.out.println("login reset------------" + WebTable.onlineAccount());
                                                oldWebSocket.close();
                                            } else {
                                                System.out.println("正常登陆");
                                            }
                                            WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangeAccountId(accountId).ChangeNickname(nickname));
                                            WebTable.onlineAccountAdd(accountId, webSocket);
                                            System.out.println("login go------------" + WebTable.connectMap());
                                            System.out.println("login go------------" + WebTable.onlineAccount());
                                            goHallProcess(connectEnsureID, webSocket, eb);
                                        } else {
                                            webSocket.writeBinaryMessage(Buffer.buffer(CodeMsgTranslate.genErrorBytes("连接异常，连接关闭")));
                                            webSocket.close();
                                        }

                                    }
                                    byte[] loginBin = CodeMsgTranslate.encode(MsgScheme.AMsg.Head.Login_Response, resBody);
                                    webSocket.writeBinaryMessage(Buffer.buffer(loginBin));
                                } else
                                    webSocket.writeBinaryMessage(Buffer.buffer(CodeMsgTranslate.genErrorBytes("账号服务异常!!!!")));
                            });


                            break;
                        case CreateRoom_Request:
                            if (b) {
                                if (connectionMsg.position().equals("inHall")) {
                                    {
                                        WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("creatingRoom").ChangeStatus("free"));
                                        eb.send(Channels.createRoom(), mapAccountId, ar ->
                                        {
                                            if (ar.succeeded()) {
                                                String s1 = ar.result().body().toString();
//                                            System.out.println("创建回复为：：：" + s1);
                                                JSONObject crInfo =
                                                        JSONObject.parseObject(s1);

                                                String aid = crInfo.getString("id");
//                                            System.out.println("创建者为：：：" + aid);
                                                int RoomId = crInfo.getInteger("room");
                                                boolean creatingRoom = WebTable.connectMap().get(connectEnsureID).get().position().equals("creatingRoom");
                                                boolean equals = aid.equals(mapAccountId);

                                                if (creatingRoom && equals) {
                                                    WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("Room" + RoomId).ChangeStatus("free"));
                                                    System.out.println("回复房间:" + RoomId + "开启ok--开启者：" + aid);
                                                    byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.CreateRoom_Response).setCreateRoomResponse(MsgScheme.CreateRoomResponse.newBuilder().setRoomId(RoomId)).build().toByteArray();

                                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));

                                                } else {
                                                    System.out.println("ws：createRoom：创建失败或玩家状态错误：" + aid);
                                                    byte[] toSend = CodeMsgTranslate.genErrorBytes("创建失败或玩家状态错误");
                                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                                }
                                            } else System.out.println("Create Server Error");
                                        });
                                    }
                                } else {
                                    byte[] toSend = CodeMsgTranslate.genErrorBytes("不可建立房间，你不在大厅中");
                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));

                                }
                            }
                            break;

                        case JoinRoom_Request: {
                            if (b) {
                                if (WebTable.connectMap().get(connectEnsureID).get().position().equals("inHall")) {
                                    WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("findingRoom").ChangeStatus("free"));
                                    //向大厅请求一个有位置的房间号
                                    eb.send(Channels.findRoom(), mapAccountId, messageAsyncResult -> {
                                        if (messageAsyncResult.succeeded() && !messageAsyncResult.result().body().equals("fail")) {

                                            String roomId = messageAsyncResult.result().body().toString();
                                            //请求到房间成功后，开始进入房间
                                            WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("joiningRoom").ChangeStatus(roomId));
                                            eb.send(Channels.joinRoomNum() + roomId, mapAccountId, messageAsyncResult1 -> {
                                                //房间回复ok，则记录在房间的状态
                                                if (WebTable.connectMap().get(connectEnsureID).get().position().equals("joiningRoom")) {
                                                    if (messageAsyncResult1.succeeded() && messageAsyncResult1.result().body().equals("ok")) {
                                                        byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.JoinRoom_Response).setJoinRoomResponse(MsgScheme.JoinRoomResponse.newBuilder().setRoomId(Integer.valueOf(roomId))).build().toByteArray();
                                                        responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                                        WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("Room" + roomId).ChangeStatus("free"));
                                                    } else {
                                                        byte[] toSend = CodeMsgTranslate.genErrorBytes("房间出问题，不可进入");
                                                        responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                                        WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("inHall").ChangeStatus("free"));
                                                    }
                                                } else {
                                                    byte[] bytes = CodeMsgTranslate.genErrorBytes("加入状态有误！！");
                                                    webSocket.writeBinaryMessage(Buffer.buffer(bytes));
                                                }

                                            });

                                        } else {
                                            byte[] toSend = CodeMsgTranslate.genErrorBytes(MsgScheme.ErrorResponse.ErrorType.NO_ROOM_TO_JOIN, "没有剩余的空房间,请创建房间");
                                            responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                            WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangePosition("inHall").ChangeStatus("free"));
                                        }
                                    });
                                } else {
                                    byte[] toSend = CodeMsgTranslate.genErrorBytes("不可加入房间，你不在大厅中");
                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                }
                            }
                            break;
                        }
                        case QuitRoom_Request: {
                            if (b) {
                                if (connectionMsg.position().startsWith("Room")) {
                                    JSONObject whoAndReason = new JSONObject();
                                    whoAndReason.put("id", mapAccountId);
                                    whoAndReason.put("reason", "normal");
                                    String whoAndReasonMsg = whoAndReason.toJSONString();
                                    WebTable.connectMapAdd(connectEnsureID, connectionMsg.ChangeStatus("quitingRoom"));
                                    eb.send(Channels.quitRoom() + WebTable.connectMap().get(connectEnsureID).get().position(), whoAndReasonMsg, messageAsyncResult ->
                                    {
                                        if (messageAsyncResult.succeeded() && WebTable.connectMap().get(connectEnsureID).get().status().equals("quitingRoom")) {
                                            goHallProcess(connectEnsureID, webSocket, eb);
                                        } else {
                                            byte[] bytes = CodeMsgTranslate.genErrorBytes("房间错误，请重启");
                                            webSocket.writeBinaryMessage(Buffer.buffer(bytes));
                                            webSocket.close();
                                        }
                                    });


                                    byte[] toSend = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.QuitRoom_Response).setQuitRoomResponse(MsgScheme.QuitRoomResponse.newBuilder()).build().toByteArray();
                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                } else {

                                    byte[] toSend = CodeMsgTranslate.genErrorBytes("不可退出房间，你不在房间中");
                                    responseWebSocket.writeBinaryMessage(Buffer.buffer(toSend));
                                }
                            }
                        }
                        break;

                        case GetReady_Request:
                            if (b) {
                                if (connectionMsg.position().startsWith("Room")) {
                                    body.put("accountId", mapAccountId);
                                    eb.send(Channels.readyRoom() + connectionMsg.position(), body.toString(), messageAsyncResult -> {
                                        String s1 = messageAsyncResult.result().body().toString();
                                        JSONObject jsonObject1 = JSONObject.parseObject(s1);
                                        String yourStatus = jsonObject1.getString("yourStatus");
                                        if (yourStatus.equals(MsgScheme.StatusInRoom.GAMING.toString())) {
                                            JSONArray accountIds = jsonObject1.getJSONArray("accountIds");
//                                            System.out.println("当前玩家有~~~~~~~" + accountIds);
                                            accountIds.forEach(jsonObj -> {
                                                String s2 = jsonObj.toString();
                                                if (WebTable.onlineAccount().contains(s2)) {
                                                    Option<ServerWebSocket> serverWebSocketOption = WebTable.onlineAccount().get(s2);
                                                    String socketBin = WebTable.getSocketBin(serverWebSocketOption);
                                                    if (WebTable.connectMap().contains(socketBin)) {
                                                        ConnectionMsg roomConnectionMsg = WebTable.connectMap().get(socketBin).get();
                                                        WebTable.connectMapAdd(socketBin, roomConnectionMsg.ChangeStatus("play"));
                                                    }
                                                }
                                            });

                                        }
                                        byte[] encode1 = CodeMsgTranslate.encode(MsgScheme.AMsg.Head.GetReady_Response, jsonObject1);
                                        webSocket.writeBinaryMessage(Buffer.buffer(encode1));
                                    });
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

