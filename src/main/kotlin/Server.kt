import com.delta.GameBoard
import com.delta.GameLogic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Server {
    val gameLogic : GameLogic = GameLogic(GameBoard(15))

    private val webSocketServer = WebSocketServer()

    private val httpServer = HttpServer(gameLogic, webSocketServer::broadcast)

    suspend fun start() {
        println("ip adress is ${Utils.getIpAddress()}")
        coroutineScope {
            launch {
                httpServer.start(wait = false)
            }
            launch {
                webSocketServer.start(wait = true)
            }
        }
    }
}