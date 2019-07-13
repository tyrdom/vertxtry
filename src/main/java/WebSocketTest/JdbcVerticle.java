package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import javafx.event.Event;

public class JdbcVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
// 获取到数据库连接的客户端
        JDBCClient jdbcClient = new JdbcUtils(vertx).getDbClient();
        String sql = "USE test";
        // 构造参数
//        JsonArray params = new JsonArray().add(18);
        // 执行查询
        jdbcClient.query(sql, qryRes -> {
            if (qryRes.succeeded()) {
                // 获取到查询的结果，Vert.x对ResultSet进行了封装
                ResultSet resultSet = qryRes.result();
                // 把ResultSet转为List<JsonObject>形式

                // 输出结果
                System.out.println("use ok" + resultSet);
                JDBCLib.tableCheckAllAndCreate(jdbcClient);
            } else {

                System.out.println("查询数据库出错！" + qryRes.cause().getMessage() + "将创建新数据库");
                if (qryRes.cause().getMessage().startsWith("Unknown")) {
                    String createDBSql = "CREATE DATABASE test";
                    jdbcClient.query(createDBSql, resultSetAsyncResult -> {
                        if (resultSetAsyncResult.succeeded()) {
                            ResultSet resultSet = resultSetAsyncResult.result();

                            // 输出结果
                            System.out.println("create ok" + resultSet);
                        } else {
                            System.out.println("！！！数据库再次错误！！！" + resultSetAsyncResult.cause().getMessage());
                        }
                    });
                }
            }
        });

        EventBus eb = vertx.eventBus();
        eb.consumer("loginGame", msg -> {
            JSONObject accountAndPassword = JSONObject.parseObject(msg.body().toString());
            String accountId = accountAndPassword.getString("accountId");
            String password = accountAndPassword.getString("password");

        });

        eb.consumer(Channels.createAccount(), msg -> {
        });
    }
}
