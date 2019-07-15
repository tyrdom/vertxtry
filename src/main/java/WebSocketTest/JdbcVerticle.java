package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.jdbc.JDBCClient;

public class JdbcVerticle extends AbstractVerticle {
    private void initSqlProcess(JDBCClient jdbcClient, String sql) {
        jdbcClient.query(sql, qryRes -> {
            if (qryRes.succeeded()) {
                // 获取到查询的结果，Vert.x对ResultSet进行了封装

                // 把ResultSet转为List<JsonObject>形式

                // 输出结果
                System.out.println("use ok");

                JDBCLib.tableCheckAllAndCreate(jdbcClient);
                if (!JDBCLib.readSqlRow(jdbcClient, SqlConfig.accountId(), SqlConfig.testARow().accountId(), SqlConfig.accountBaseTable()).ok()) {
                    JDBCLib.accountCreate(jdbcClient, SqlConfig.testARow());
                }
            } else {

                System.out.println("查询数据库出错！" + qryRes.cause().getMessage() + "将创建新数据库");
                if (qryRes.cause().getMessage().startsWith("Unknown")) {
                    String createDBSql = "CREATE DATABASE " + SqlConfig.database() + ";";
                    jdbcClient.query(createDBSql, resultSetAsyncResult -> {
                        if (resultSetAsyncResult.succeeded()) {


                            // 输出结果
                            System.out.println("create ok:");
                            initSqlProcess(jdbcClient, sql);
                        } else {
                            System.out.println("！！！数据库再次错误！！！" + resultSetAsyncResult.cause().getMessage());
                        }
                    });
                }
            }
        });


    }

    @Override
    public void start() throws Exception {
// 获取到数据库连接的客户端
        JDBCClient jdbcClient = new JdbcUtils(vertx).getDbClient();
        String sql = "USE " + SqlConfig.database() + ";";
        // 构造参数
//        JsonArray params = new JsonArray().add(18);
        // 执行查询
        initSqlProcess(jdbcClient, sql);


        EventBus eb = vertx.eventBus();
        eb.consumer(Channels.loginGame(), msg ->

        {
            JSONObject accountAndPassword = JSONObject.parseObject(msg.body().toString());
            String accountId = accountAndPassword.getString("accountId");
            String password = accountAndPassword.getString("password");

        });

        eb.consumer(Channels.createAccount(), msg ->

        {
            JSONObject createAccountMsg = JSONObject.parseObject(msg.body().toString());
            String accountId = createAccountMsg.getString("accountId");
            String password = createAccountMsg.getString("password");
            String weChat = createAccountMsg.getString("weChat");
            Long phone = createAccountMsg.getLong("phone");
            JDBCLib.CreateRes createRes = JDBCLib.accountCreate(jdbcClient, new SqlConfig.AccountBaseData(accountId, password, accountId, weChat, phone));
            msg.reply(createRes.toJSON().toJSONString());
        });
    }
}
