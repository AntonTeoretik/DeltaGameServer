import Utils.getIpAddress
import com.delta.GameLogic
import com.delta.PlayerID
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlin.reflect.KSuspendFunction1


class HttpServer(
    private val gameLogic: GameLogic,
    private val broadcast: KSuspendFunction1<String, Unit>
) {
    data class Player(val id: String, val pwd: String)

    private val players = mutableListOf<Player>()
    private var assignedIds: Map<Player, PlayerID>? = null
    private var gameStarted = false

    private val gson = Gson()

    private suspend fun registerPlayer(id: String): Player {
        if (gameStarted) throw Exception("Game already started, you can't join it")

        if (players.any { it.id == id }) throw Exception("The player with a given id is already in the game")

        val player = Player(
            id,
            UUID.randomUUID().toString()
        )

        if (addPlayer(player)) return player
        throw Exception("The game is already started")

    }

    private suspend fun addPlayer(player: Player): Boolean {
        var success = false
        if (players.size < PlayerID.values().size) {
            players.add(player)
            success = true
        }
        tryToStartGame()
        return success
    }

    private suspend fun tryToStartGame() {
        if (!gameStarted && players.size == PlayerID.values().size) {
            gameStarted = true
            assignPlayerIds()
            broadcastGameState()
        }
    }

    private fun assignPlayerIds() {
        val shuffledIds = PlayerID.values().toMutableList().shuffled()
        assignedIds = players.subList(0, shuffledIds.size).zip(shuffledIds).toMap()
    }

    // API
    private val httpServer = embeddedServer(Netty, host = getIpAddress(), port = 8080) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }

        routing {
            get("/") {
                call.respondText("Game API is running!", ContentType.Text.Plain)
            }

            post("/login") {
                try {
                    validatePassword(call.parameters)
                    val id = call.parameters["id"]!!
                    val player = registerPlayer(id)
                    call.respondText(gson.toJson(player), status = HttpStatusCode.OK)
                } catch (e: Exception) {
                    respondException(call, e)
                }
            }

            post("/placeCell") {
                try {
                    val player = validatePlayer(call.parameters)
                    val playerId = assignedIds!![player]!!
                    val raw = call.parameters["raw"]!!.toInt()
                    val col = call.parameters["col"]!!.toInt()

                    if (gameLogic.placeCell(raw, col, playerId))
                        respondOk(call)
                    else
                        call.respondText("You can't place a cell now", status = HttpStatusCode.BadRequest)
                } catch (e: Exception) {
                    respondException(call, e)
                }
            }

            post("/endPlayersTurn") {
                try {
                    val player = validatePlayer(call.parameters)
                    val playerId = assignedIds!![player]!!
                    if (gameLogic.endPlayersTurn(playerId))
                        respondOk(call)
                    else
                        call.respondText("You can't finish your turn now", status = HttpStatusCode.BadRequest)
                } catch (e: Exception) {
                    respondException(call, e)
                }
            }

            get("/playerID") {
                try {
                    val player = validatePlayer(call.parameters)
                    val playerId = assignedIds!![player]!!
                    call.respondText(gson.toJson(playerId), status = HttpStatusCode.OK)
                } catch (e: Exception) {
                    respondException(call, e)
                }
            }

            get("/getWinners") {
                try {
                    val winners = gameLogic.getWinners()
                    if (winners != null) {
                        call.respondText(winners.joinToString(separator = ","))
                    } else {
                        call.respondText("No winners yet")
                    }
                } catch (e: Exception) {
                    respondException(call, e)
                }
            }
        }
    }

    private suspend fun broadcastGameState() {
        broadcast(gameLogic.toJson())
    }

    private suspend fun respondOk(call: ApplicationCall) {
        broadcastGameState()
        call.respondText("Ok", status = HttpStatusCode.OK)
    }

    private suspend fun respondException(call: ApplicationCall, e: Exception) {
        call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
    }

    private fun validatePlayer(parameters: Parameters): Player {
        if (!gameStarted) throw Exception("Game is not started yet")
        val potentialPlayer = Player(parameters["id"]!!, parameters["pwd"]!!)
        if (potentialPlayer in players) return potentialPlayer
        throw Exception("No such player or credentials are wrong")
    }

    private fun validatePassword(parameters: Parameters) {
        if (!parameters["server_pwd"]?.equals("Delta!!!")!!)
            throw Exception("Wrong server password")
    }


    fun start(wait: Boolean) {
        httpServer.start(wait)
    }
}