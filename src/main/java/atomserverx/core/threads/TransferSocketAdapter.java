package atomserverx.core.threads;

import plethora.print.log.State;
import plethora.utils.Sleeper;
import atomserverx.core.HostClient;
import atomserverx.core.HostSign;
import atomserverx.core.SocketOperator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;

import static atomserverx.AtomServerX.*;

public class TransferSocketAdapter implements Runnable {
    public static final CopyOnWriteArrayList<HostSign> hostList = new CopyOnWriteArrayList<>();
    public static int SO_TIMEOUT = 500;

    public static void startThread() {
        new Thread(new TransferSocketAdapter()).start();
    }

    @Override
    public void run() {
        try {
            hostServerTransferServerSocket = new ServerSocket(HOST_CONNECT_PORT);
            while (true) {
                Socket host = hostServerTransferServerSocket.accept();
                new Thread(() -> {
                    ObjectInputStream objectInputStream;
                    try {
                        out:
                        for (int i = 1; i <= 2; i++) {
                            objectInputStream = new ObjectInputStream(host.getInputStream());
                            ObjectOutputStream objectOutputStream = new ObjectOutputStream(host.getOutputStream());
                            int pretendedPort = objectInputStream.readInt();
                            for (HostClient hostClient : availableHostClient) {
                                if (hostClient.getOutPort() == pretendedPort) {
                                    hostList.add(new HostSign(pretendedPort, host, objectInputStream, objectOutputStream));
                                    break out;
                                }
                            }
                            SocketOperator.closeSocket(host);
                        }
                    } catch (IOException e) {
                        SocketOperator.closeSocket(host);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            sayInfo(State.ERROR, "TransferSocketAdapter", "Can not blind the port , it's Occupied ?");
            System.exit(-1);
        }
    }

    public static HostSign getThisHostClientHostSign(int port) throws SocketTimeoutException {
        for (int i = 0; i < SO_TIMEOUT / 10; i++) {
            for (HostSign hostSign : hostList) {
                if (hostSign.port() == port) {
                    hostList.remove(hostSign);
                    return hostSign;
                }
            }
            Sleeper.sleep(10);
        }
        throw new SocketTimeoutException();
    }
}
