package atomserverx;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class SocketOperator {
    public static final String COMMAND_PREFIX = ":>";

    private SocketOperator() {
    }

    public static void sendStr(HostClient hostClient, String str) throws IOException {
        ObjectOutputStream objectOutputStream = hostClient.getWriter();
        objectOutputStream.writeObject(hostClient.getAESUtil().encrypt(str.getBytes(StandardCharsets.UTF_8)));
        objectOutputStream.flush();

    }

    public static String receiveStr(HostClient hostClient) throws IOException {
        try {
            ObjectInputStream objectInputStream = hostClient.getReader();
            return new String(hostClient.getAESUtil().decrypt((byte[]) objectInputStream.readObject()), StandardCharsets.UTF_8);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void closeSocket(Socket... socket) {

        for (Socket socket1 : socket) {
            try {
                socket1.close();
            } catch (Exception ignore) {
            }
        }

    }

    public static void closeSocket(ServerSocket clientServerSocket) {
        try {
            clientServerSocket.close();
        } catch (IOException ignored) {
        }
    }

    public static void closeSocket(Socket hostServerHook, ServerSocket clientServerSocket) {
        try {
            hostServerHook.close();
            clientServerSocket.close();
        } catch (IOException ignored) {
        }
    }

    public static void sendCommand(HostClient hostClient, String command) {
        try {
            sendStr(hostClient, COMMAND_PREFIX + command);
        } catch (Exception ignored) {
        }
    }

    public static String getInternetAddressAndPort(Socket socket) {
        return socket.getInetAddress().toString().replaceAll("/", "") + ":" + socket.getPort();
    }

    public static String getIP(Socket socket) {
        return socket.getInetAddress().toString().replaceAll("/", "");
    }

}
