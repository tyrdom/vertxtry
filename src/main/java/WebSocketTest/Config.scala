package WebSocketTest

import io.vertx.lang.scala.json.JsonObject


object Config {
  val maxPlayer = 2

  val jso = new JsonObject
  jso.put("url", "jdbc:mysql://localhost:3306?serverTimezone=UTC")
  jso.put("driver_class", "com.mysql.cj.jdbc.Driver")
  jso.put("user", "root")
  jso.put("password", "123456")

}
