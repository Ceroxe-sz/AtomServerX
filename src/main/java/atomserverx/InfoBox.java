package atomserverx;

import plethora.print.log.State;

import java.net.Socket;

import static atomserverx.AtomServerX.sayInfo;
import static atomserverx.SocketOperator.getInternetAddressAndPort;

public class InfoBox {
    public static void sayHostClientDiscInfo(HostClient hostClient, String subject) {
        sayInfo(State.INFO, subject, "Detected hostClient on " + hostClient.getAddressAndPort() + " has been disconnected !");
    }

    public static void sayClientConnectBuildUpInfo(HostClient hostClient, Socket client) {
        sayInfo("Connection: " + getInternetAddressAndPort(client) + " -> " + hostClient.getAddressAndPort() + " build up !");
    }

    public static void sayClientConnectDestroyInfo(HostClient hostClient, Socket client) {
        sayInfo("Connection: " + getInternetAddressAndPort(client) + " -> " + hostClient.getAddressAndPort() + " destroyed !");
    }

    public static void sayClientSuccConnecToChaSerButHostClientTimeOut(HostClient hostClient) {
        sayInfo("A client successfully connect to the channel server but host client from " + hostClient.getAddressAndPort() + " time out.");
    }
}
