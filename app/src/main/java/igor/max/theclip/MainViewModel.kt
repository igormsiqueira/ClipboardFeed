package igor.max.theclip

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.InetAddresses
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.PrintWriter
import java.net.ConnectException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Enumeration


open class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    private lateinit var currentClient: Socket
    private lateinit var currentDeviceIpAddress: String
    private val nsdManager = application.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager =
        application.getSystemService(ConnectivityManager::class.java)


    private var serviceCType = "_http._tcp" // Change to your desired service type
    private var selectedName = "TheClip-BoardManager"
    private var mPort = 33452
    private val TAG = "TAGASD"
    val clipboardFeed = MutableLiveData(mutableListOf<String>())
    val mode = MutableLiveData(Mode.UNSPECIFIED)
    val serverInfo = MutableLiveData<NsdServiceInfo>()

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        }

        override fun onDiscoveryStarted(serviceType: String) {
        }

        override fun onDiscoveryStopped(serviceType: String) {
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceName == selectedName) {
                this@MainViewModel.onServiceFound(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        }
    }

    init {
        nsdManager.discoverServices(serviceCType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun onServiceFound(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")
            serverInfo.postValue(serviceInfo)
//            clientConnectionToServer(serviceInfo)
        }
    }
    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            startAcceptingConnections()
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            throw IllegalStateException("onRegistrationFailed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        }
    }

    private fun startAcceptingConnections() {
        val server = ServerSocket(mPort)
//        val connectedClient = server.accept()
        // val output = PrintWriter(connectedClient.getOutputStream(), true)

//        while (true) {
//            val input = BufferedReader(InputStreamReader(connectedClient.inputStream))
//
//            server.use { it ->
//            }
//
//            input.readLine()?.let {
//                clipboardFeed.value.orEmpty().toMutableList().apply {
//                    add(it)
//                    clipboardFeed.postValue(this)
//                }
//            }
//
//            println("OK-CONNECTED-MIKE received ${input.readLine()}")
//        }

        server.use {
            while (true) {
                println("waiting for client")
                val socket = server.accept()
                System.out.printf(
                    "client connected from: %s%n",
                    socket.remoteSocketAddress.toString()
                )
                val connectionHandshake =
                    BufferedReader(InputStreamReader(socket.getInputStream())).readLine()
                appendOutput(connectionHandshake)
                socket.use { connectedSocket ->

                    val input = connectedSocket.getInputStream()

                    val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val out = PrintStream(socket.getOutputStream())
                    var inputLine: String
                    while (inputStream.readLine().also { inputLine = it } != null) {
                        println("received:")
                        println(inputLine)
//                        val outputStringBuilder = StringBuilder("")
//                        val inputLineChars = inputLine.toCharArray()
//                        for (c in inputLineChars) outputStringBuilder.append(Character.toChars(c.code + 1))
//                        out.println(outputStringBuilder)

                        appendOutput(inputLine)
                    }
                }
            }
        }
    }

    private fun appendOutput(inputLine: String) {
        clipboardFeed.value.orEmpty().toMutableList().apply {
            add(inputLine)
            clipboardFeed.postValue(this)
        }
    }

    private fun clientConnectionToServer(serviceInfo: NsdServiceInfo) {
        try {
            val serviceName = serviceInfo.serviceName
            val hostAddress = serviceInfo.host.hostAddress

            val client = Socket(hostAddress, mPort)
            val output = PrintWriter(client.getOutputStream(), true)
            currentClient = client
            output.println("Hi! Connection from ${getDeviceIp()} \uD83D\uDC4B")
        } catch (e: ConnectException) {
            e.printStackTrace()
        }

        /*  try {
              val socket = Socket(hostAddress, mPort)
              PrintWriter(socket.getOutputStream(),true).write("The Clip")
              val outputStream = socket.getOutputStream()
              val message = "Hello, service!"
              outputStream.write(message.toByteArray())

               Receive data from the service
              val inputStream = socket.getInputStream()
              val buffer = ByteArray(1024)
              val bytesRead = inputStream.read(buffer)
              val receivedMessage = String(buffer, 0, bytesRead)

              throw IllegalAccessException(receivedMessage.toString())

              // Process the received data
              // ...

              // Close the socket when communication is done
              socket.close()
          } catch (e: IOException) {
              e.printStackTrace()
          }*/

    }

    fun getDeviceIp(): String {

//        var ip =
//            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)?.linkAddresses?.find {
//                isIpValid(it.address.address.toString())
//            }
//
//        currentDeviceIpAddress = ip?.address?.address.toString()
//        connectivityManager.getLinkProperties(connectivityManager.activeNetwork)?.linkAddresses?.takeIf { linkAddresses ->
//            val validLink = linkAddresses.filter { linkAddress ->
//                isIpValid(linkAddress.address.address.toString())
//            }
//            currentDeviceIpAddress = validLink.first().address?.address.toString()
//            return currentDeviceIpAddress
//        }

//        connectivityManager.getLinkProperties(connectivityManager.activeNetwork)?.linkAddresses?.let {
//            it.forEach {
//                if (isIpValid(it.address.address.toString())) {
//                    return it.address.address.toString()
//                }
//            }
//        }
//
//        currentDeviceIpAddress =
//            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)!!.linkAddresses[0].address.hostAddress!!
        currentDeviceIpAddress = getIPv4Address().toString()
        return currentDeviceIpAddress
    }
    fun getIPv4Address(): String? {
        try {
            val networkInterfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface: NetworkInterface = networkInterfaces.nextElement()
                val inetAddresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress: InetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.indexOf(':') < 0) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    fun startServer() {
        mode.postValue(Mode.SERVER)
        viewModelScope.launch {
            val serviceInfo = NsdServiceInfo().apply {
                this.serviceType = serviceCType
                this.serviceName = selectedName
                this.port = mPort
                host = InetAddress.getByName(getDeviceIp())
            }

            withContext(Dispatchers.IO) {
                nsdManager.registerService(
                    serviceInfo,
                    NsdManager.PROTOCOL_DNS_SD,
                    registrationListener
                )
            }
        }
    }

    fun startClient() {
        mode.postValue(Mode.CLIENT)
        if (serverInfo.value == null) {
            nsdManager.stopServiceDiscovery(discoveryListener)
            nsdManager.discoverServices(serviceCType, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                }

                override fun onDiscoveryStarted(serviceType: String) {
                }

                override fun onDiscoveryStopped(serviceType: String) {
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceName == selectedName) {
                        this@MainViewModel.onServiceFound(serviceInfo)
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                }
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                clientConnectionToServer(serverInfo.value!!)
            }
        }
    }

    fun sendText(text: String) {
        if (::currentClient.isInitialized) {
            CoroutineScope(Dispatchers.IO).launch {
                val output = PrintWriter(
                    withContext(Dispatchers.IO) { currentClient.getOutputStream() },
                    true
                )
                output.println(text)
                println("Sending $text")
            }
        }
    }

    fun isIpValid(ip: String): Boolean {
        val ipRegex = """^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""".toRegex()
        return ip.matches(ipRegex)

//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            InetAddresses.isNumericAddress(ip)
//        } else {
//            return Patterns.IP_ADDRESS.matcher(ip).matches()
//        }
    }

}