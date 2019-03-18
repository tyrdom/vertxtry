package WebSocketTest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

public class RoomVerticle extends AbstractVerticle {
    //玩家id，玩家状态
    private Map<String, String> players = new HashMap<>(16);
    //房间状态
    private String roomStatus = "standBy";
    //最大人数
    private final int maxPlayer = Config.maxPlayer();


    @Override
    public void start() throws Exception {
        String hostPlayerId = config().getString("host");
        players.put(hostPlayerId, "standBy");
        System.out.println("Room is ok:" + hostPlayerId);
        final Integer roomId = config().getInteger("roomId");

        EventBus eb = vertx.eventBus();

        eb.consumer("offLineRoom" + roomId, who -> {
            String offId = who.body().toString();
            if (players.containsKey(offId)) {
                if (roomStatus.equals("standBy")) {
                    players.remove(offId);
                    eb.send("leftRoom", roomId);
                    if (players.size() == 0) {
                        eb.send("emptyRoom", roomId);
                    }
                }

            }
        });

        eb.consumer("joinRoom" + roomId, msg -> {
            String who = msg.body().toString();
            if (players.size() < maxPlayer) {
                players.put(who, "standBy");
                msg.reply("ok");
            } else {
                msg.reply("full");
            }
        });

    }
}
