package WebSocketTest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class JdbcUtils {

    // 用于操作数据库的客户端
    private JDBCClient dbClient;

    public JdbcUtils(Vertx vertx) {

        // 构造数据库的连接信息
        JsonObject dbConfig = Config.jso();

        // 创建客户端
        dbClient = JDBCClient.createShared(vertx, dbConfig);


    }



    // 提供一个公共方法来获取客户端
    public JDBCClient getDbClient() {
        return dbClient;
    }

}

