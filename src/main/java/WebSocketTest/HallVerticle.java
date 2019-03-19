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

//大厅负责房间管理，创建，状态记录和消灭，向玩家提供创建，寻找房间服务
public class HallVerticle extends AbstractVerticle {

    //玩家id，玩家状态：standBy：大厅待命
    //                  房间号：找到了房号正在进入某房间
    private Map<String, String> playersInHall = new HashMap<>(16);
    //所有房间的信息 房间id，创建者，人数，verticleId
    private Map<Integer, Triplet<String, Integer, String>> rooms = new HashMap<>(16);
    private Integer roomId = 1;
    //异步消息无法确认到达的先后顺序，所以某些消息需要保存起来
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

//        eb.consumer("test.address", message -> {
//            String mess = message.body().toString();
//            System.out.println("Received message1: " + mess);
//
//            JSONObject msg1 = JSONObject.parseObject(mess);
//
//            System.out.println("Received message3: " + msg1);
//
//
//             Now send back reply
//
//        });
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

        //房间发出的某玩家离开房间
        eb.consumer("leftRoom", msg -> {
            JSONObject whoAndRoomIdAndReason = JSONObject.parseObject(msg.body().toString());
            String id = whoAndRoomIdAndReason.getString("id");
            Integer roomId = whoAndRoomIdAndReason.getInteger("room");
            String reason = whoAndRoomIdAndReason.getString("reason");
            Triplet<String, Integer, String> oldStatus = rooms.get(roomId);
            int nowPlayerNum = oldStatus.getValue1() - 1;
            if (nowPlayerNum <= 0) {
                String verticleId = oldStatus.getValue2();
                vertx.undeploy(verticleId, res -> {
                    if (res.succeeded()) {
                        rooms.remove(roomId);
                        System.out.println("房间：" + roomId + "销毁成功！！");
                    }
                });
            } else {
                Triplet<String, Integer, String> newStatus = new Triplet<>(oldStatus.getValue0(), nowPlayerNum, oldStatus.getValue2());
                rooms.put(roomId, newStatus);
            }
            if (reason.equals("offLine")) {
                playersInHall.remove(id);

            } else if (reason.equals("normal")) {//如果是正常退出，没有断开，则保存一个回到大厅的状态
                playersInHall.put(id, "leavingRoom");
            } else {
                playersInHall.remove(id);
            }
        });

        //房间发出，某玩家已经进入房间成功
        eb.consumer("haveInRoom", msg -> {
            JSONObject playerIdAndRoomId = JSONObject.parseObject(msg.body().toString());
            String playerId = playerIdAndRoomId.getString("id");
            String roomId = playerIdAndRoomId.getString("room");
            //确认成功进入，把玩家移除大厅
            if (playersInHall.get(playerId).equals(roomId)) {
                playersInHall.remove(playerId);
            }
        });

        eb.consumer("quitHall", msg -> {
            playersInHall.remove(msg.body().toString());

        });

        eb.consumer("cancelLogin", msg -> {
            JSONObject whoAndReason = JSONObject.parseObject(msg.body().toString());

            String who = whoAndReason.getString("id");

            if (!playersInHall.containsKey(who)) {
                loginCancelPlayers.add(who);
            } else {
                playersInHall.remove(who);
            }
        });

        eb.consumer("cancelCreate", msg -> {
                    JSONObject whoAndReason = JSONObject.parseObject(msg.body().toString());

                    String who = whoAndReason.getString("id");
                    String reason = whoAndReason.getString("reason");

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
                    //  如果是掉线引起的取消创建，那么该玩家也会离开大厅
                    if (reason.equals("offLine")) {
                        playersInHall.remove(who);
                    }
                }
        );

        eb.consumer("createRoom", msg -> {
            String Id = msg.body().toString();
            //在大厅的，并且不在掉线取消创建用户集中的，则去创建房间
            if (!createCancelPlayers.remove(Id) && playersInHall.containsKey(Id)) {

                JsonObject config = new JsonObject().put("host", Id).put("roomId", roomId);

                DeploymentOptions opt = new DeploymentOptions().setConfig(config).setWorker(true);
                vertx.deployVerticle(new RoomVerticle(), opt, stringAsyncResult -> {
                    if (stringAsyncResult.succeeded()) {
                        String roomVerticleId = stringAsyncResult.result();
                        playersInHall.remove(Id);

                        rooms.put(roomId, new Triplet<>(Id, 1, roomVerticleId));
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("id", msg.body());
                        roomInfo.put("room", roomId);
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
            JSONObject whoAndReason = JSONObject.parseObject(msg.body().toString());

            String who = whoAndReason.getString("id");
            String reason = whoAndReason.getString("reason");
            //如果玩家在standBy状态，说明寻找房间消息还未到达，记录取消消息，停止该玩家的寻找房间服务
            if (playersInHall.get(who).equals("standBy")) {
                findCancelPlayers.add(who);
                msg.reply("early");
            } else {
                String RoomId = playersInHall.get(who);
                msg.reply(RoomId);
            }
            //  如果是因为断线而取消，则会让玩家离开大厅
            if (reason.equals("offLine")) {
                playersInHall.remove(who);
            }

        });

        eb.consumer("findRoom", msg ->

                {

                    String who = msg.body().toString();
                    //如果没有取消定位的消息
                    if (!findCancelPlayers.remove(who)) {
                        playersInHall.put(who, "findingRoom");
                        boolean roomFound = false;
                        for (Map.Entry<Integer, Triplet<String, Integer, String>> entry : rooms.entrySet()) {
                            //找一个人数未满的房间
                            if (entry.getValue().getValue1() < maxPlayer) {
                                //找到房间回复房间号并订下位置，房间人数暂时+1，以免其他人再进入
                                Integer foundRoomId = entry.getKey();
                                Triplet<String, Integer, String> oldStatus = entry.getValue();
                                Triplet<String, Integer, String> newStatus = new Triplet<>(oldStatus.getValue0(), oldStatus.getValue1() + 1, oldStatus.getValue2());
                                rooms.put(foundRoomId, newStatus);
                                msg.reply(entry.getKey());
                                //在大厅记录玩家正在进入房间的状态，在取消消息到达时，可以告知去哪个房间取消订位
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
