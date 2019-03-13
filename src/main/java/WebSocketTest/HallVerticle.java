package WebSocketTest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.JsonObject;


import java.util.HashSet;

import java.util.Set;

public class HallVerticle extends AbstractVerticle {

    private Set<Object> players = new HashSet<>(16);

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

            if (players.add(msg.body())) {
//                for (Object ob : players) {
//                   System.out.println(ob);
//                }
            }
        });

        eb.consumer("playerOffLine", msg -> {
            if (players.remove(msg.body())) {
//                for (Object ob : players) {
//                    System.out.println(ob);
//                }
            }
        });


        eb.consumer("createRoom", msg -> {
            if (players.contains(msg.body()))
            {



            JsonObject config = new JsonObject().put("host", msg.body());
            DeploymentOptions opt = new DeploymentOptions().setConfig(config).setWorker(true);
            vertx.deployVerticle(new RoomVerticle(), opt);
            msg.reply("ok");
            players.remove(msg.body());}
            else {
                msg.reply("fail");
            }

        });

//        eb.consumer("joinRoom",msg->{
//            eb.send("")
//        });
        System.out.println("Receiver ready!");


    }
}
