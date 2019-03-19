package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import java.util.*;

public class RoomVerticle extends AbstractVerticle {

    private Map<String, String> players = new HashMap<>(16);
    //房间状态 standBy：准备 ，gaming： 游戏中
    private String roomStatus = "standBy";
    //最大人数
    private final Integer maxPlayer = Config.maxPlayer();

    private Set<String> somebodyWantToQuit = new HashSet<>(16);

    @Override
    public void start() {
        String hostPlayerId = config().getString("host");
        players.put(hostPlayerId, "standBy");
        System.out.println("Room is ok:" + hostPlayerId);
        final Integer roomId = config().getInteger("roomId");

        EventBus eb = vertx.eventBus();


        eb.consumer("quitRoom" + roomId, msg -> {
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
                    eb.send("leftRoom", whoAndRoomIdAndReasonMsg);

                } else {
                    somebodyWantToQuit.add(who);
                    eb.send("leftRoom", whoAndRoomIdAndReasonMsg);
                }

            }

        });

        eb.consumer("joinRoom" + roomId, msg -> {
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
                    eb.send("haveInRoom", sendMsg);
                } else {
                    msg.reply("full");
                }
            }
            msg.reply("fail");
        });

    }
}
