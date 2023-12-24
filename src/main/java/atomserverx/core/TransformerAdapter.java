package atomserverx.core;

import atomserverx.core.threads.TransferSocketAdapter;
import atomserverx.core.threads.Transformer;

import java.io.IOException;
import java.net.Socket;

import static atomserverx.AtomServerX.sayInfo;
import static atomserverx.core.InfoBox.sayClientConnectBuildUpInfo;
import static atomserverx.core.InfoBox.sayClientSuccConnecToChaSerButHostClientTimeOut;
import static atomserverx.core.SocketOperator.closeSocket;
import static atomserverx.core.SocketOperator.getInternetAddressAndPort;

public class TransformerAdapter {
    public static void createNewTransformService(HostClient finalHostClient,Socket client){
        HostSign hostSign;
        Socket host;
        try {
            hostSign = TransferSocketAdapter.getThisHostClientHostSign(finalHostClient.getOutPort());
            host = hostSign.host();
        } catch (IOException e) {//if host client timeout
            sayClientSuccConnecToChaSerButHostClientTimeOut(finalHostClient);
            sayInfo("Killing client's side connection: " + getInternetAddressAndPort(client));
            closeSocket(client);
            return;
        }
        sayClientConnectBuildUpInfo(finalHostClient, client);//say connection build up info

        //start real transfer service
        Transformer.startThread(finalHostClient, host, client, hostSign);
    }
}
