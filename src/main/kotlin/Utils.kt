import java.net.InetAddress

object Utils {
    fun getIpAddress(): String {
        val inetAddress = InetAddress.getLocalHost()
        return inetAddress.hostAddress
    }
}