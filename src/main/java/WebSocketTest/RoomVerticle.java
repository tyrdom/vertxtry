package WebSocketTest;

import io.vertx.core.AbstractVerticle;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

public class RoomVerticle extends AbstractVerticle {
    private Map<String,String> players = new HashMap<>(16);

    @Override
    public void start() throws Exception {
        String hostPlayerId = config().getString("host");
        players.put("host",hostPlayerId);
        System.out.println("Room is ok:"+ hostPlayerId);


    }
}
