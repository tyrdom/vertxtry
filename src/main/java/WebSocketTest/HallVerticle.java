package WebSocketTest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.JsonObject;


import java.util.HashMap;
import java.util.HashSet;

import java.util.Map;
import java.util.Set;

public class HallVerticle extends AbstractVerticle {

    private Set<String> players = new HashSet<>(16);
    private Map<Integer,Integer> rooms = new HashMap<>(16);
    private Integer roomId = 1;

    @Override
    public void start() {

        EventBus eb = vertx.eventBus();

        eb.consumer("test.address", message -> {
            String mess = message.body().toString();
//            System.out.println("Received message1: " + mess);

            JSONObject msg1 = JSONObject.parseObject(mess);

            System.out.println("Received message3: " + msg1);


            // Now send back reply

        });
        eb.consumer("player.inHall", msg -> {

            if (players.add(msg.body().toString())) {

                msg.reply("ok");
            } else {
                msg.reply("fail");
            }
        });

        eb.consumer("playerOffLine", msg -> {
            if (players.remove(msg.body().toString())) {
                msg.reply("ok");
            } else {
                msg.reply("fail");
            }
        });


        eb.consumer("createRoom", msg -> {
            String Id =msg.body().toString();
            if (players.contains(Id)) {


                JsonObject config = new JsonObject().put("host", Id).put("roomId", roomId);

                DeploymentOptions opt = new DeploymentOptions().setConfig(config).setWorker(true);
                vertx.deployVerticle(new RoomVerticle(), opt);
                players.remove(Id);
                rooms.put(roomId,1);
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("Id", msg.body());
                roomInfo.put("roomId", roomId);
                msg.reply(roomInfo);
                if (roomId < 10000)
                    roomId++;
                else roomId = 1;
            } else {
                msg.reply("fail");
            }

        });

        eb.consumer("joinRoom", msg -> {
            eb.send("a", "a");
        });
        System.out.println("Receiver ready!");


    }
}
