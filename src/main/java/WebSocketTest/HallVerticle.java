package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.JsonObject;
import org.javatuples.Triplet;
import scala.AnyVal;


import java.util.HashMap;
import java.util.HashSet;

import java.util.Map;
import java.util.Set;

public class HallVerticle extends AbstractVerticle {

    private Set<String> players = new HashSet<>(16);
    //所有房间的信息 房间id，创建者，人数，verticleId
    private Map<Integer, Triplet<String, Integer, String>> rooms = new HashMap<>(16);
    private Integer roomId = 1;
    private Set<String> createCancelPlayers = new HashSet<>(16);
    private Set<String> loginCancelPlayers = new HashSet<>(16);

    private final int maxPlayer = Config.maxPlayer();

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
            String who = msg.body().toString();
            if (!loginCancelPlayers.remove(who)) {
                if (players.add(who)) {
                    System.out.println("大厅：进入大厅成功" + who);
                    msg.reply(who);
                } else {
                    msg.reply("fail");
                }
            }

            msg.reply("fail");
        });

        eb.consumer("quitHall", msg -> {
            if (!players.remove(msg.body().toString())) {

                System.out.println("！！！！有一个用户在大厅掉线，但是大厅没有这个用户，一定有个bug");
            }
        });

        eb.consumer("cancelLogin", who -> {
            String player = who.body().toString();
            if (!players.remove(player)) {
                loginCancelPlayers.add(player);
            }
        });

        eb.consumer("cancelCreate", msg -> {
                    String who = msg.body().toString();

                }
        );

        eb.consumer("createRoom", msg -> {
            String Id = msg.body().toString();
            if (players.contains(Id)) {


                JsonObject config = new JsonObject().put("host", Id).put("roomId", roomId);

                DeploymentOptions opt = new DeploymentOptions().setConfig(config).setWorker(true);
                vertx.deployVerticle(new RoomVerticle(), opt, stringAsyncResult -> {
                    if (stringAsyncResult.succeeded()) {
                        String roomVerticleId = stringAsyncResult.result();
                        players.remove(Id);
                        rooms.put(roomId, new Triplet<>(Id, 1, roomVerticleId));
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("Id", msg.body());
                        roomInfo.put("roomId", roomId);
                        msg.reply(roomInfo.toJSONString());


                        if (roomId < 10000)
                            roomId++;
                        else roomId = 1;
                    } else {
                        msg.reply("createFail");
                    }
                });

            } else {
                msg.reply("fail");
            }

        });

        eb.consumer("joinRoom", who -> {
            for (Map.Entry<Integer, Triplet<String, Integer, String>> entry : rooms.entrySet()) {
                if (entry.getValue().getValue1() < maxPlayer) {
                    String roomChannel = "joinRoom" + entry.getKey();
                    eb.send(roomChannel, who.body());
                    break;
                }
            }

        });
        System.out.println("Receiver ready!");


    }
}
