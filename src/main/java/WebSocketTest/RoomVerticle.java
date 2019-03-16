package WebSocketTest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

public class RoomVerticle extends AbstractVerticle {
    private Map<String, String> players = new HashMap<>(16);
    private String roomStatus = "standBy";
    public final int maxPlayer = Config.maxPlayer();
//    private Queue<String> joinQueue = new ArrayDeque<>();

    @Override
    public void start() throws Exception {
        String hostPlayerId = config().getString("host");
        players.put(hostPlayerId, "standBy");
        System.out.println("Room is ok:" + hostPlayerId);
        final int roomId = config().getInteger("roomId");

        EventBus eb = vertx.eventBus();
        eb.consumer("playerOffLine", msg -> {
                    String offId = msg.body().toString();
                    if (players.containsKey(offId)) {
                        if (roomStatus.equals("standBy")) {
                        }

                    }
                }
        );

        eb.consumer("joinRoom" + roomId, msg -> {
            String who = msg.body().toString();
            if (players.size() < maxPlayer) {
                players.put(who, "standBy");
                eb.send("joinRoom", who);
            } else {

                eb.send("cantJoin", roomId);
            }
        });

    }
}
