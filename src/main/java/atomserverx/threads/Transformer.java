package atomserverx.threads;

import asia.ceroxe.management.bufferedFile.SizeCalculator;
import asia.ceroxe.thread.ThreadManager;
import atomserverx.HostClient;
import atomserverx.HostSign;
import atomserverx.LanguageData;
import atomserverx.exceptions.NoMoreNetworkFlowException;

import java.io.*;
import java.net.Socket;

import static atomserverx.AtomServerX.IS_DEBUG_MODE;
import static atomserverx.InfoBox.sayClientConnectDestroyInfo;
import static atomserverx.InfoBox.sayHostClientDiscInfo;
import static atomserverx.SocketOperator.*;
import static atomserverx.Vault.removeVaultOnAll;

public class Transformer implements Runnable {
    public static int BUFFER_LEN = 256;
    public static int TELL_RATE_MIB = 10;
    private final HostClient hostClient;
    private final Socket host;
    private final Socket client;
    private HostSign hostSign;


    private Transformer(HostClient hostClient, Socket host, Socket client, HostSign hostSign) {
        this.hostClient = hostClient;
        this.host = host;
        this.client = client;
        this.hostSign = hostSign;
    }

    public static void startThread(HostClient hostClient, Socket host, Socket client, HostSign hostSign) {
        new Thread(new Transformer(hostClient, host, client, hostSign)).start();
    }

    public static void transferDataToAtomServer(HostClient hostClient, Socket client, Socket host, HostSign hostSign, double[] aTenMibSize) {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(client.getInputStream());
            ObjectOutputStream objectOutputStream = hostSign.objectOutputStream();

            int len;
            byte[] data = new byte[BUFFER_LEN];
            while ((len = bufferedInputStream.read(data)) != -1) {
//                System.out.println("len = " + len);
                byte[] enData = hostClient.getAESUtil().encrypt(data, 0, len);
                objectOutputStream.writeObject(enData);
                objectOutputStream.flush();
                objectOutputStream.writeInt(len);
                objectOutputStream.flush();

                hostClient.getVault().mineMib(SizeCalculator.byteToMib(enData.length));
                tellRestRate(hostClient, aTenMibSize, enData.length, hostClient.getLangData());//tell the host client the rest rate.
            }

            objectOutputStream.writeObject(null);//tell host client is end!
            host.shutdownOutput();
            client.shutdownInput();

        } catch (IOException e) {

            if (IS_DEBUG_MODE) {
                e.printStackTrace();
            }
            try {

                hostSign.objectOutputStream().writeObject(null);//tell host client is end!
                host.shutdownOutput();
                client.shutdownInput();

            } catch (IOException ignore) {
            }

        } catch (NoMoreNetworkFlowException e) {
            removeVaultOnAll(hostClient.getVault());
            kickAllWithMsg(hostClient, host, client);
        }
        System.gc();
    }

    public static void transferDataToOuterClient(HostClient hostClient, Socket host, Socket client, HostSign hostSign, double[] aTenMibSize) {
        try {
            ObjectInputStream objectInputStream = hostSign.objectInputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(client.getOutputStream());

            byte[] data;
            while ((data = (byte[]) objectInputStream.readObject()) != null) {
//                System.out.println("data.length = " + data.length);
                byte[] deData = hostClient.getAESUtil().decrypt(data);
                bufferedOutputStream.write(deData, 0, objectInputStream.readInt());
                bufferedOutputStream.flush();

                hostClient.getVault().mineMib(SizeCalculator.byteToMib(deData.length));
                tellRestRate(hostClient, aTenMibSize, deData.length, hostClient.getLangData());//tell the host client the rest rate.
            }

            host.shutdownInput();
            client.shutdownOutput();

        } catch (IOException | ClassNotFoundException e) {
            if (IS_DEBUG_MODE) {
                e.printStackTrace();
            }
            try {

                host.shutdownInput();
                client.shutdownOutput();

            } catch (IOException ignore) {
            }
        } catch (NoMoreNetworkFlowException e) {
            removeVaultOnAll(hostClient.getVault());
            kickAllWithMsg(hostClient, host, client);
        }
        System.gc();
    }

    public static void tellRestRate(HostClient hostClient, double[] aTenMibSize, int len, LanguageData languageData) throws IOException {
        if (aTenMibSize[0] < TELL_RATE_MIB) {//tell the host client the rest rate.
            aTenMibSize[0] = aTenMibSize[0] + SizeCalculator.byteToMib(len);
        } else {
            sendStr(hostClient, languageData.THIS_ACCESS_CODE_HAVE + hostClient.getVault().getRate() + languageData.MB_IF_FLOW_LEFT);
            aTenMibSize[0] = 0;
        }
    }

    private static void kickAllWithMsg(HostClient hostClient, Socket host, Socket client) {
        hostClient.close();
        closeSocket(client);
        closeSocket(client, host);
        try {
            sendCommand(hostClient, "exit");
            sayHostClientDiscInfo(hostClient, "Transformer");
        } catch (Exception e) {
            sayHostClientDiscInfo(hostClient, "Transformer");
        }
    }

    @Override
    public void run() {
        try {
            final double[] aTenMibSize = {0};
            Runnable clientToHostClientThread = () -> transferDataToAtomServer(hostClient, client, host, hostSign, aTenMibSize);
            Runnable hostClientToClientThread = () -> transferDataToOuterClient(hostClient, host, client, hostSign, aTenMibSize);
            ThreadManager threadManager = new ThreadManager(clientToHostClientThread, hostClientToClientThread);
            threadManager.startAll();
            closeSocket(client, host);
            sayClientConnectDestroyInfo(hostClient, client);
        } catch (Exception ignore) {
            closeSocket(client, host);
            sayClientConnectDestroyInfo(hostClient, client);
        }
        System.gc();
    }

}
