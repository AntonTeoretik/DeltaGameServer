import kotlinx.coroutines.runBlocking


fun main() {
    val server = Server()
    runBlocking {
        server.start()
    }
}
