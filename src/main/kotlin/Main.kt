import Utils.getIpAddress
import com.delta.GameBoard
import com.delta.GameLogic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Server {
    val gameLogic : GameLogic = GameLogic(GameBoard(10))

    private val webSocketServer = WebSocketServer()

    private val httpServer = HttpServer(gameLogic, webSocketServer::broadcast)

    suspend fun start() {
        println("ip adress is ${getIpAddress()}")
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


fun main() {
    val server = Server()
    runBlocking {
        server.start()
    }
}
