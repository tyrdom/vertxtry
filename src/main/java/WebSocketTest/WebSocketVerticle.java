package WebSocketTest;


import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;

import io.vertx.ext.web.Router;

import scala.Tuple2;


import java.util.HashMap;
import java.util.Map;

public class WebSocketVerticle extends AbstractVerticle {
    // 保存每一个连接到服务器的通道
    private Map<String, Tuple2<String, ServerWebSocket>> connectionMap = new HashMap<>(16);

    public boolean checkID(String id) {
        return connectionMap.containsKey(id);
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            routingContext.response().sendFile("html/ws.html");
        });
        webSocketMethod(server);
        server.requestHandler(router::accept).listen(8080);
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        vertx.deployVerticle(HallVerticle.class.getName(), options);
    }

    public void webSocketMethod(HttpServer server) {
        server.websocketHandler(webSocket -> { // 获取每一个链接的ID
            String id = webSocket.binaryHandlerID();
            EventBus eb = vertx.eventBus();
            // 判断当前连接的ID是否存在于map集合中，如果不存在则添加进map集合中
            if (!checkID(id)) {
                connectionMap.put(id, Tuple2.apply("loginHall", webSocket));

                eb.send("player.inHall", id);
            }
            webSocket.closeHandler(handler -> {
                        eb.send("playerOffLine", id);
                        connectionMap.remove(id);
                    }
            );
            eb.consumer("createRoom",msg ->{
                
            });

            //　WebSocket 连接
            webSocket.frameHandler(handler -> {
                String textData = handler.textData();
                String currID = webSocket.binaryHandlerID();
                if (textData.equals("create")) {
                    eb.send("createRoom", currID);
                }
                //给非当前连接到服务器的每一个WebSocket连接发送消息
                for (Map.Entry<String, Tuple2<String, ServerWebSocket>> entry : connectionMap.entrySet()) {




                    if (currID.equals(entry.getKey())) {
                        continue;
                    } /* 发送文本消息 文本信息似乎不支持图片等二进制消息
                    若要发送二进制消息，可用writeBinaryMessage方法
                    */

                    JSONObject msg = new JSONObject();
                    msg.put("name", "abc");
                    msg.put("age", 11);
                    String output = msg.toJSONString();
                    System.out.println(output);
                    eb.send("test.address", output);
                    entry.getValue()._2.writeTextMessage("用户" + id + ":" + textData + "\r");
                }


            });
        });
    }
}
