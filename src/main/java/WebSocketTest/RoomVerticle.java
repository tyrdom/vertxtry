package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import gameplayLib.GamePlayGround;
import gameplayLib.GamePlayGroundInit;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import msgScheme.MsgScheme;

import java.util.*;
import java.util.function.BiConsumer;

public class RoomVerticle extends AbstractVerticle {
    //玩家id 玩家状态 ：standBy 待命
    //                  ready 准备完成
    //                  gaming 游戏中
    private int tempId = -1;
    private String roomName;

    private int genTempId() {
        tempId = (tempId + 1) % maxPlayer;
        return tempId;

    }

    //    private Map<String, String> players = new HashMap<>(16);
    private Map<String, PlayerStatus> playerStatusMap = new HashMap<>(16);

    private void newPlayer(String aid) {
        PlayerStatus status = PlayerStatus.getStatus(genTempId(), aid);
        if (status.webSocketServer().isEmpty()) {
            System.out.println("账号错误，没有此玩家");
        } else
            playerStatusMap.put(aid, status);
    }

    private void changePlayerStatus(String who, MsgScheme.StatusInRoom newStatus) {
        PlayerStatus orDefault = playerStatusMap.getOrDefault(who, PlayerStatus.zero());
        if (orDefault == PlayerStatus.zero()) {
            System.out.println("账号错误，没有此玩家");
        } else playerStatusMap.put(who, orDefault.changeStatus(newStatus));
    }

    private void broadcastPlayerStatus() {

        Collection<PlayerStatus> values = playerStatusMap.values();
        if (playerStatusMap.isEmpty()) {
            System.out.println("房间空掉，等待销毁");
        } else {
            playerStatusMap.forEach((k, v) ->
                    {
                        int tempId1 = v.tempId();
                        MsgScheme.RoomPlayerStatusBroadcast.Builder builder = MsgScheme.RoomPlayerStatusBroadcast.newBuilder();
                        values.forEach(x -> {
                            int i = x.tempId();
                            String nickname = x.nickname();
                            MsgScheme.StatusInRoom status = x.status();
                            MsgScheme.RoomPlayerStatusBroadcast.OnePlayerInRoom.Builder builder1 = MsgScheme.RoomPlayerStatusBroadcast.OnePlayerInRoom.newBuilder().setNickName(nickname).setStatus(status).setTempId(i);
                            builder.addPlayerStatusList(builder1);
                        });
                        builder.setYourTempId(tempId1);
                        MsgScheme.AMsg.Builder aMsg = MsgScheme.AMsg.newBuilder().setHead(MsgScheme.AMsg.Head.RoomPlayerStatus_Broadcast).setRoomPlayerStatusBroadcast(builder);
                        byte[] bytes = aMsg.build().toByteArray();
                        ServerWebSocket serverWebSocket = v.webSocketServer().get();
                        serverWebSocket.writeBinaryMessage(Buffer.buffer(bytes));
                    }

            );
            System.out.println("广播了消息：" + values);
        }

    }

    //房间状态 standBy：准备 ，gaming： 游戏中
    private String roomStatus = "standBy";
    //最大人数
    private final Integer maxPlayer = Config.maxPlayer();
    // 在进入消息前如果退出消息先到，那么记录想要退出的人
//    private Set<String> somebodyWantToQuit = new HashSet<>(16);

    @Override
    public void start() {
        String hostPlayerId = config().getString("host");
//        players.put(hostPlayerId, "standBy");
        newPlayer(hostPlayerId);
        System.out.println("Room is ok:" + hostPlayerId);
        final Integer roomId = config().getInteger("roomId");
        roomName = "Room" + roomId;
        EventBus eb = vertx.eventBus();


        eb.consumer(Channels.quitRoom() + roomName, msg -> {
            String text = msg.body().toString();
            JSONObject whoAndReason = JSONObject.parseObject(text);
            String who = whoAndReason.getString("id");
            String reason = whoAndReason.getString("reason");
            JSONObject whoAndRoomIdAndReason = new JSONObject();
            whoAndRoomIdAndReason.put("id", who);
            whoAndRoomIdAndReason.put("room", roomId);
            whoAndRoomIdAndReason.put("reason", reason);
            String whoAndRoomIdAndReasonMsg = whoAndRoomIdAndReason.toJSONString();
            //房间在待命状态

            System.out.println("quit:" + who);
            if (playerStatusMap.containsKey(who)) {
                if (roomStatus.equals("gaming")) {
                    changePlayerStatus(who, MsgScheme.StatusInRoom.OFFLINE);
                    msg.reply("play");


                } else {
//                    players.remove(who);
                    playerStatusMap.remove(who);
                    System.out.println(roomId + "room:" + who + "leaving room");
                    eb.send(Channels.leftRoom(), whoAndRoomIdAndReasonMsg);

                    msg.reply("ok");
                }
            } else {
                msg.reply("error");
            }
            broadcastPlayerStatus();
        });

        eb.consumer(Channels.joinRoomNum() + roomId, msg -> {
            String who = msg.body().toString();
            JSONObject playerIdAndRoomId = new JSONObject();
            playerIdAndRoomId.put("id", who);
            playerIdAndRoomId.put("room", roomId.toString());
            String sendMsg = playerIdAndRoomId.toJSONString();

            if (playerStatusMap.size() < maxPlayer) {
//
                newPlayer(who);
                msg.reply("ok");


                eb.send(Channels.haveInRoom(), sendMsg);
                if (playerStatusMap.size() == maxPlayer) {
                    roomStatus = "full";
                } else {
                    //TODO roomError
                }
            } else {
                msg.reply("full");
            }
            broadcastPlayerStatus();
        });

        eb.consumer(Channels.readyRoom() + roomName, msg ->

        {
            String s = msg.body().toString();
            JSONObject jsonObject = JSONObject.parseObject(s);
            String who = jsonObject.getString("accountId");
            Boolean isReady = jsonObject.getBoolean("isReady");
            JSONObject entries = new JSONObject();
            MsgScheme.StatusInRoom statusInRoom = playerStatusMap.getOrDefault(who, PlayerStatus.zero()).status();
            if (playerStatusMap.containsKey(who)
                    && (statusInRoom == MsgScheme.StatusInRoom.READY
                    || statusInRoom == MsgScheme.StatusInRoom.STANDBY)) {
                if (isReady)
                    changePlayerStatus(who, MsgScheme.StatusInRoom.READY);
                else {
                    changePlayerStatus(who, MsgScheme.StatusInRoom.STANDBY);
                    entries.put("yourStatus", MsgScheme.StatusInRoom.STANDBY.toString());
                    msg.reply(entries.toString());
                }
                //test Array

                //

                boolean allReady = true;
                for (Map.Entry<String, PlayerStatus> entry : playerStatusMap.entrySet()) {
                    if (!entry.getValue().status().equals(MsgScheme.StatusInRoom.READY)) {
                        allReady = false;
                    }
                }
                if (allReady && playerStatusMap.size() == Config.maxPlayer()) {

                    roomStatus = "gaming";
                    for (Map.Entry<String, PlayerStatus> entry : playerStatusMap.entrySet()) {
                        changePlayerStatus(entry.getKey(), MsgScheme.StatusInRoom.GAMING);
                    }
                    entries.put("yourStatus", MsgScheme.StatusInRoom.GAMING.toString());
                    Set<String> strings = playerStatusMap.keySet();
                    entries.put("accountIds", strings);
                    msg.reply(entries.toString());

                    GamePlayGround gamePlayGround = GamePlayGroundInit.gamePlayGroundInit();
                    System.out.println("init playground ok!");

                    int[] cIds = gameplayLib.Config.standardCIds();
                    String[] pIds = playerStatusMap.keySet().toArray(new String[maxPlayer]);
                    gamePlayGround.initPlayGround(pIds, cIds);

                } else {
                    entries.put("yourStatus", MsgScheme.StatusInRoom.READY.toString());
                    msg.reply(entries.toString());
                }

            } else {
                entries.put("yourStatus", MsgScheme.StatusInRoom.ERROR.toString());
                msg.reply(entries.toString());
            }

            broadcastPlayerStatus();
        });
    }
}
