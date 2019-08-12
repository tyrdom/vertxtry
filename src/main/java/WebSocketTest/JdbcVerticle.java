package WebSocketTest;

import com.alibaba.fastjson.JSONObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import msgScheme.MsgScheme;

import java.util.List;

public class JdbcVerticle extends AbstractVerticle {
    private void initSqlProcess(JDBCClient jdbcClient, String sql) {
        jdbcClient.query(sql, qryRes -> {
            if (qryRes.succeeded()) {
                // 获取到查询的结果，Vert.x对ResultSet进行了封装

                // 把ResultSet转为List<JsonObject>形式

                // 输出结果
                System.out.println("use ok");

                JDBCLib.tableCheckAllAndCreate(jdbcClient);
                String readSql = JDBCLib.readSqlStringARowBy1Limit(Account_base.account_id(), Account_base.testARow().accountId(), Account_base.account_base_table());
                jdbcClient.query(readSql, qRes -> {
                            if (qRes.succeeded()) {
                                if (qRes.result().getRows().isEmpty()) {
                                    Account_base.AccountBaseData testARow = Account_base.testARow();
                                    String s = testARow.accountCreateSqlString();
                                    jdbcClient.query(s, res ->
                                    {
                                        if (res.succeeded()) {
                                            System.out.println("=======testCreateOk=======");
                                        } else System.out.println("==========test account fail!============");
                                    });
                                } else System.out.println("read test ok:" + qRes.result().getRows());
                            } else System.out.println("======testAccountReadFail!!!!============");
                        }
                );

            } else {

                System.out.println("查询数据库出错！" + qryRes.cause().getMessage() + "将创建新数据库");

                String createDBSql = "CREATE DATABASE " + Database.database();
                jdbcClient.query(createDBSql, resultSetAsyncResult -> {
                    if (resultSetAsyncResult.succeeded()) {
                        // 输出结果
                        System.out.println("create database ok:");
                        initSqlProcess(jdbcClient, sql);
                    } else {
                        System.out.println("！！！数据库再次错误！！！" + resultSetAsyncResult.cause().getMessage());
                    }
                });
            }
        });
    }

    @Override
    public void start() throws Exception {
// 获取到数据库连接的客户端
        JDBCClient jdbcClient = new JdbcUtils(vertx).getDbClient();
        String sql = "USE " + Database.database() + ";";
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
            String s = JDBCLib.accountCheckSqlString(accountId, password);
            jdbcClient.query(s, res -> {
                if (res.succeeded() && !res.result().getRows().isEmpty()) {

                    JsonObject entries = res.result().getRows().get(0);
                    String nickname = entries.getString("nickname");
                    System.out.println("get from db!!!!!!!" + entries);
                    JsonObject jsonObject = new JsonObject().put("reason", MsgScheme.LoginResponse.Reason.OK.toString()).put("nickname", nickname);

                    msg.reply(jsonObject);
                } else {

                    msg.reply(new JsonObject().put("reason", MsgScheme.LoginResponse.Reason.WRONG_PASSWORD.toString()).put("nickname", ""));
                }
            });
        });

        eb.consumer(Channels.createAccount(), msg ->
        {
            JSONObject createAccountMsg = JSONObject.parseObject(msg.body().toString());
            String accountId = createAccountMsg.getString("accountId");
            String password = createAccountMsg.getString("password");
            String weChat = createAccountMsg.getString("weChat");
            Long phone = createAccountMsg.getLong("phone");
            System.out.println("creating Account Msg OK:" + accountId + password);
            boolean passOk = JDBCLib.passwordSchemeCheck(password);
            boolean accountOk = JDBCLib.accountIdSchemeCheck(accountId);
            if (passOk && accountOk) {
                Account_base.AccountBaseData accountBaseData = new Account_base.AccountBaseData(accountId, password, accountId, weChat, phone);
                String checkSql = JDBCLib.readSqlStringARowBy1Limit(Account_base.account_id(), accountBaseData.accountId(), Account_base.account_base_table());
                jdbcClient.query(checkSql, res -> {
                    if (res.succeeded()) {
                        if (res.result().getRows().isEmpty()) {
                            System.out.println("creating!!!!");
                            String createSql = accountBaseData.accountCreateSqlString();
                            jdbcClient.query(createSql, res1 -> {
                                if (res1.succeeded()) {
                                    System.out.println("create ok!!!!");
                                    msg.reply(new JsonObject().put("reason", MsgScheme.CreateAccountResponse.Reason.OK.toString()));
                                } else {
                                    msg.reply(new JsonObject().put("reason", MsgScheme.CreateAccountResponse.Reason.OTHER.toString()));
                                }
                            });
                        } else {
                            System.out.println("already exist!!!");
                            msg.reply(new JsonObject().put("reason", MsgScheme.CreateAccountResponse.Reason.ALREADY_EXIST.toString()));
                        }
                    } else {
                        msg.reply(new JsonObject().put("reason", MsgScheme.CreateAccountResponse.Reason.OTHER.toString()));
                    }
                });

            } else
                msg.reply(new JsonObject().put("reason", MsgScheme.CreateAccountResponse.Reason.NO_GOOD_PASSWORD.toString()));
        });
    }
}
