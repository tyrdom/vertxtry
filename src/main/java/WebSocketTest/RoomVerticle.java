package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import gameplayLib.GamePlayGround;
import gameplayLib.GamePlayGroundInit;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import scala.Array;

import java.util.*;

public class RoomVerticle extends AbstractVerticle {
    //玩家id 玩家状态 ：standBy 待命
    //                  ready 准备完成
    //                  gaming 游戏中

    private Map<String, String> players = new HashMap<>(16);
    //房间状态 standBy：准备 ，gaming： 游戏中
    private String roomStatus = "standBy";
    //最大人数
    private final Integer maxPlayer = Config.maxPlayer();
    // 在进入消息前如果退出消息先到，那么记录想要退出的人
    private Set<String> somebodyWantToQuit = new HashSet<>(16);

    @Override
    public void start() {
        String hostPlayerId = config().getString("host");
        players.put(hostPlayerId, "standBy");
        System.out.println("Room is ok:" + hostPlayerId);
        final Integer roomId = config().getInteger("roomId");

        EventBus eb = vertx.eventBus();


        eb.consumer(Channels.quitRoom() + "Room" + roomId, msg -> {
            JSONObject whoAndReason = JSONObject.parseObject(msg.body().toString());

            String who = whoAndReason.getString("id");

            String reason = whoAndReason.getString("reason");
            JSONObject whoAndRoomIdAndReason = new JSONObject();
            whoAndRoomIdAndReason.put("id", who);
            whoAndRoomIdAndReason.put("room", roomId);
            whoAndRoomIdAndReason.put("reason", reason);
            String whoAndRoomIdAndReasonMsg = whoAndRoomIdAndReason.toJSONString();
            //房间在待命状态
            if (roomStatus.equals("standBy")) {
                if (players.containsKey(who)) {
                    players.remove(who);
                    eb.send(Channels.leftRoom(), whoAndRoomIdAndReasonMsg);
                    //如果在待命状态,人没有进入 则把先到退出消息加入
                } else {
                    somebodyWantToQuit.add(who);
                    eb.send(Channels.leftRoom(), whoAndRoomIdAndReasonMsg);
                }

            } else {
                //TODO 重连 将不发出leftRoom消息，等待账号重连
                players.remove(who);
                eb.send(Channels.leftRoom(), whoAndRoomIdAndReasonMsg);
            }

        });

        eb.consumer(Channels.joinRoomNum() + roomId, msg -> {
            String who = msg.body().toString();
            JSONObject playerIdAndRoomId = new JSONObject();
            playerIdAndRoomId.put("id", who);
            playerIdAndRoomId.put("room", roomId.toString());
            String sendMsg = playerIdAndRoomId.toJSONString();
            //加入的人如果在要退出的人中，则不添加此玩家，回复fail
            if (!somebodyWantToQuit.remove(who)) {
                if (players.size() < maxPlayer) {
                    players.put(who, "standBy");
                    msg.reply("ok");
                    eb.send(Channels.haveInRoom(), sendMsg);
                    if (players.size() == maxPlayer) {
                        roomStatus = "full";
                    } else if (players.size() > maxPlayer) {
                        //TODO roomError

                    }
                } else {
                    msg.reply("full");
                }
            }
            msg.reply("fail");
        });

        eb.consumer(Channels.readyRoom() + roomId, msg ->

        {
            String who = msg.body().toString();
            if (roomStatus.equals("full")) {
                if (players.containsKey(who)) {
                    players.put(who, "ready");
                    boolean allReady = true;
                    for (Map.Entry<String, String> entry : players.entrySet()) {
                        if (!entry.getValue().equals("ready")) {
                            allReady = false;
                        }
                    }
                    if (allReady) {
                        msg.reply("gameStart");
                        roomStatus = "gaming";
                        for (Map.Entry<String, String> entry : players.entrySet()) {
                            players.put(entry.getKey(), "gaming");
                        }
                        GamePlayGround gamePlayGround = GamePlayGroundInit.gamePlayGroundInit();

                        int[] cIds = gameplayLib.Config.standardCIds();
                        String[] pIds = players.keySet().toArray(new String[maxPlayer]);
                        gamePlayGround.initPlayGround(pIds, cIds);

                    } else {
                        msg.reply("readyOk");
                    }
                } else {
                    msg.reply("error");
                }
            } else {
                msg.reply("notFull");
            }
        });
    }
}
