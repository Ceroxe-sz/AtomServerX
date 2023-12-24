package atomserverx.core;

import plethora.print.log.State;
import plethora.security.encryption.AESUtil;
import plethora.utils.Sleeper;
import atomserverx.core.exceptions.IllegalConnectionException;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;

import static atomserverx.AtomServerX.availableHostClient;
import static atomserverx.AtomServerX.sayInfo;
import static atomserverx.core.InfoBox.sayHostClientDiscInfo;
import static atomserverx.core.SocketOperator.sendCommand;
import static atomserverx.core.SocketOperator.sendStr;
import static atomserverx.core.Vault.removeVaultOnAll;

public final class HostClient {
    public static int SAVE_DELAY = 3000;//3s
    public static int DETECTION_DELAY = 1000;
    public static int FAILURE_LIMIT = 5;
    private boolean isStopped = false;
    private Vault vault = null;
    private final Socket hostServerHook;
    private ServerSocket clientServerSocket = null;
    private ObjectOutputStream hostServerHookWriter = null;
    private ObjectInputStream hostServerHookReader = null;
    private LanguageData languageData = new LanguageData();
    private int outPort = -1;
    public static int AES_KEY_SIZE = 128;
    private AESUtil aesUtil = new AESUtil(AES_KEY_SIZE);//AES-128

    public HostClient(Socket hostServerHook) throws IOException, IllegalConnectionException {
        this.hostServerHook = hostServerHook;

        HostClient.initConnection(this);

        HostClient.enableAutoSaveThread(this);
        HostClient.enableVaultDetectionTread(this);
    }

    private static void initConnection(HostClient hostClient) throws IllegalConnectionException {
        try {

            hostClient.hostServerHookWriter = new ObjectOutputStream(hostClient.getHostServerHook().getOutputStream());
            hostClient.hostServerHookReader = new ObjectInputStream(hostClient.getHostServerHook().getInputStream());

            Cipher enCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            enCipher.init(Cipher.ENCRYPT_MODE, (PublicKey) hostClient.hostServerHookReader.readObject());
            hostClient.getWriter().writeObject(enCipher.doFinal(hostClient.getAESUtil().getKeyBytes()));
            hostClient.getWriter().flush();

        } catch (Exception e) {
            hostClient.close();
            IllegalConnectionException.throwException(hostClient.hostServerHook);
        }
    }

    private static void enableAutoSaveThread(HostClient hostClient) {
        Thread a = new Thread(() -> {
            while (true) {
                if (hostClient.getVault() != null && !hostClient.isStopped) {
                    hostClient.getVault().save();
                }
                if (hostClient.isStopped) {
                    break;
                }
                Sleeper.sleep(SAVE_DELAY);
            }
            System.gc();
        });
        a.start();
    }

    private static void enableVaultDetectionTread(HostClient hostClient) {
        Thread a = new Thread(() -> {
            while (true) {
                if (hostClient.getVault() != null && !hostClient.isStopped && hostClient.getVault().isOutOfDate()) {
                    sayInfo("The vault " + hostClient.getVault().getName() + " is out of date !");
                    try {
                        sendStr(hostClient, hostClient.getLangData().THE_VAULT + hostClient.getVault().getName() + hostClient.getLangData().ARE_OUT_OF_DATE);
                        sendCommand(hostClient, "exit");
                        sayHostClientDiscInfo(hostClient, "VaultDetectionTread");
                    } catch (IOException e2) {
                        sayHostClientDiscInfo(hostClient, "VaultDetectionTread");
                    }
                    removeVaultOnAll(hostClient.getVault());
                    hostClient.getVault().getFile().delete();
                    hostClient.close();
                    break;
                } else {
                    Sleeper.sleep(DETECTION_DELAY);
                }
                if (hostClient.isStopped) {
                    break;
                }
            }
            System.gc();
        });
        a.start();
    }

    public void close() {
        availableHostClient.remove(this);
        try {
            hostServerHookWriter.writeObject(null);//tell the client is exit!
        } catch (IOException ignore) {
        }

        try {
            hostServerHook.close();
            if (clientServerSocket != null) {
                clientServerSocket.close();
            }

        } catch (Exception ignore) {
        }
        this.isStopped = true;
        this.gc();
    }

    public void enableCheckAliveThread() {
        HostClient hostClient = this;

        final int[] failureTime = {0};
        new Thread(() -> {
            while (failureTime[0] < FAILURE_LIMIT) {
                try {
                    Object obj = hostClient.hostServerHookReader.readObject();
                    if (obj == null) {
                        failureTime[0]++;
                    } else {
                        failureTime[0] = 0;
                    }
                } catch (Exception e) {
                    failureTime[0]++;
                }
                Sleeper.sleep(1000);
            }
            System.gc();
        }).start();

        new Thread(() -> {
            while (failureTime[0] < FAILURE_LIMIT) {
                Sleeper.sleep(1000);
            }

            hostClient.close();
            isStopped = true;

            sayInfo(State.INFO, "CheckAliveThread", "Detected hostClient on " + hostClient.getAddressAndPort() + " has been disconnected !");

            System.gc();
        }).start();

    }

    public Vault getVault() {
        return vault;
    }

    public void setVault(Vault vault) {
        this.vault = vault;
    }

    public Socket getHostServerHook() {
        return hostServerHook;
    }

    public ServerSocket getClientServerSocket() {
        return clientServerSocket;
    }

    public void setClientServerSocket(ServerSocket clientServerSocket) {
        this.clientServerSocket = clientServerSocket;
    }

    public ObjectOutputStream getWriter() {
        return hostServerHookWriter;
    }

    public ObjectInputStream getReader() {
        return hostServerHookReader;
    }

    public String getAddressAndPort() {
        return SocketOperator.getInternetAddressAndPort(hostServerHook);
    }

    public LanguageData getLangData() {
        return languageData;
    }

    public void setLangData(LanguageData languageData) {
        this.languageData = languageData;
    }

    public int getOutPort() {
        return outPort;
    }

    public void setOutPort(int outPort) {
        this.outPort = outPort;
    }

    public AESUtil getAESUtil() {
        return aesUtil;
    }

    private void gc() {
        System.gc();
    }
}
