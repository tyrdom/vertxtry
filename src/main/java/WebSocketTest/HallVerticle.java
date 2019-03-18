package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.JsonObject;
import org.javatuples.Triplet;


import java.util.HashMap;
import java.util.HashSet;

import java.util.Map;
import java.util.Set;

public class HallVerticle extends AbstractVerticle {

    private Map<String, String> playersInHall = new HashMap<>(16);
    //所有房间的信息 房间id，创建者，人数，verticleId
    private Map<Integer, Triplet<String, Integer, String>> rooms = new HashMap<>(16);
    private Integer roomId = 1;
    //在创建房间时掉线，先收到取消消息，后收到到创建消息，那么会通过createCancelPlayers记录并阻止房间创建
    private Set<String> createCancelPlayers = new HashSet<>(16);
    //在登录大厅时掉线，先收到取消消息，后收到到登录消息，那么会通过loginCancelPlayers记录并阻止玩家登录
    private Set<String> loginCancelPlayers = new HashSet<>(16);
    //在请求寻找房间时掉线，先收到取消消息，后收到到寻找消息，那么会通过findCancelPlayers记录并阻止玩家寻找房间
    private Set<String> findCancelPlayers = new HashSet<>(16);
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
        eb.consumer("playerOffLineInHall", msg ->
                playersInHall.remove(msg.body().toString()));


        eb.consumer("player.inHall", msg -> {
            String who = msg.body().toString();

            if (!loginCancelPlayers.remove(who)) {
                playersInHall.put(who, "standBy");
                System.out.println("大厅：进入大厅成功" + who);
                msg.reply(who);
            } else {
                msg.reply("loginFail");
            }
        });


        eb.consumer("leftRoom", msg -> {
        });

        eb.consumer("emptyRoom", msg ->
                {
                    Integer eRoomId = Integer.getInteger(msg.body().toString());
                    String verticleToUndeploy = rooms.get(eRoomId).getValue2();
                    vertx.undeploy(verticleToUndeploy, res -> {
                        if (res.failed()) {
                            System.out.println("空房间消灭失败！！房间号：" + eRoomId);
                        }
                    });

                }
        );

        eb.consumer("quitHall", msg -> {
            playersInHall.remove(msg.body().toString());

        });

        eb.consumer("cancelLogin", who -> {
            String player = who.body().toString();
            if (!playersInHall.containsKey(player)) {
                loginCancelPlayers.add(player);
            } else {
                playersInHall.remove(player);
            }
        });

        eb.consumer("cancelCreate", msg -> {

                    String who = msg.body().toString();
                    boolean found = false;
                    for (Map.Entry<Integer, Triplet<String, Integer, String>> entry : rooms.entrySet()) {
                        if (entry.getValue().getValue0().equals(who)) {
                            found = true;
                            vertx.undeploy(entry.getValue().getValue2(), voidAsyncResult -> {
                                if (voidAsyncResult.failed()) {
                                    createCancelPlayers.add(who);
                                }
                            });
                        }
                    }
                    if (!found) {
                        createCancelPlayers.add(who);
                    }
                }
        );

        eb.consumer("createRoom", msg -> {
            String Id = msg.body().toString();
            //在大厅的，并且不在掉线取消创建用户集中的，则去创建房间
            if (playersInHall.containsKey(Id) && !createCancelPlayers.remove(Id)) {


                JsonObject config = new JsonObject().put("host", Id).put("roomId", roomId);

                DeploymentOptions opt = new DeploymentOptions().setConfig(config).setWorker(true);
                vertx.deployVerticle(new RoomVerticle(), opt, stringAsyncResult -> {
                    if (stringAsyncResult.succeeded()) {
                        String roomVerticleId = stringAsyncResult.result();
                        playersInHall.remove(Id);
                        rooms.put(roomId, new Triplet<>(Id, 1, roomVerticleId));
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("Id", msg.body());
                        roomInfo.put("roomId", roomId);
                        msg.reply(roomInfo.toJSONString());

                        //房间号1-10000循环使用
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
        eb.consumer("cancelFind", msg -> {
            String who = msg.body().toString();
            if (playersInHall.get(who).equals("standBy")) {
                findCancelPlayers.add(who);
                msg.reply("early");
            } else {
                String RoomId = playersInHall.get(who);
                msg.reply(RoomId);
            }
        });

        eb.consumer("findRoom", msg -> {

                    String who = msg.body().toString();
                    if (findCancelPlayers.remove(who)) {
                        playersInHall.put(who, "findingRoom");
                        boolean roomFound = false;
                        for (Map.Entry<Integer, Triplet<String, Integer, String>> entry : rooms.entrySet()) {
                            if (entry.getValue().getValue1() < maxPlayer) {
                                msg.reply(entry.getKey());
                                playersInHall.put(who, entry.getKey().toString());
                                roomFound = true;
                                break;
                            }
                        }
                        if (!roomFound) {
                            msg.reply("fail");
                        }
                    }
                }

        );


        System.out.println("Receiver ready!");

    }
}
