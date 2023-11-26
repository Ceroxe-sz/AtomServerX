package atomserverx;

import plethora.management.bufferedFile.BufferedFile;
import plethora.print.log.Loggist;
import plethora.print.log.State;
import plethora.time.Time;
import plethora.utils.ArrayUtils;
import atomserverx.exceptions.*;
import atomserverx.threads.AdminThread;
import atomserverx.threads.CheckUpdateThread;
import atomserverx.threads.TransferSocketAdapter;
import atomserverx.threads.Transformer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import static atomserverx.InfoBox.*;
import static atomserverx.SocketOperator.*;
import static atomserverx.threads.CheckUpdateThread.LINUX_UPDATE_PORT;
import static atomserverx.threads.CheckUpdateThread.WINDOWS_UPDATE_PORT;

public class AtomServerX {
    public static final String CURRENT_DIR_PATH = System.getProperty("user.dir");
    public static final File VAULT_FILE_DIR = new File(CURRENT_DIR_PATH + File.separator + "vault");
    public static String EXPECTED_CLIENT_VERSION = "5.7-RELEASE";
    public static final CopyOnWriteArrayList<String> availableVersions = ArrayUtils.stringArrayToList(EXPECTED_CLIENT_VERSION.split("\\|"));

    public static int HOST_HOOK_PORT = 801;
    public static int HOST_CONNECT_PORT = 802;

    public static String LOCAL_DOMAIN_NAME = null;
    public static CopyOnWriteArrayList<Vault> vaultDatabase = new CopyOnWriteArrayList<>();
    public static ServerSocket hostServerTransferServerSocket = null;
    public static ServerSocket hostServerHookServerSocket = null;
    public static Loggist loggist;
    public static final int START_PORT = 50000;
    public static final int END_PORT = 65535;
    public static final CopyOnWriteArrayList<HostClient> availableHostClient = new CopyOnWriteArrayList<>();
    public static boolean IS_DEBUG_MODE = false;

    public static void initLoggist() {
        File logFile = new File(CURRENT_DIR_PATH + File.separator + "logs" + File.separator + Time.getCurrentTimeAsFileName(false) + ".log");
        Loggist l = new Loggist(logFile);
        l.openWriteChannel();
        AtomServerX.loggist = l;
    }

    public static void initStructure() {

        initLoggist();
        initVaultDatabase();

        ConfigOperator.readAndSetValue();

        try {
            hostServerHookServerSocket = new ServerSocket(HOST_HOOK_PORT);
            TransferSocketAdapter.startThread();
        } catch (IOException e) {
            e.printStackTrace();
            sayInfo(State.ERROR, "Main", "Can not blind the port , it's Occupied ?");
            System.exit(-1);
        }

        if (!VAULT_FILE_DIR.exists()) {
            VAULT_FILE_DIR.mkdirs();
        }

        BufferedFile logDir = new BufferedFile(CURRENT_DIR_PATH + File.separator + "logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

    }

    public static void initVaultDatabase() {
        if (VAULT_FILE_DIR.exists()) {
            File[] c = VAULT_FILE_DIR.listFiles();
            if (c == null) {
                sayInfo(State.ERROR, "Main", "No vault at all...");
                System.exit(-1);
            }
            for (File file : c) {
                vaultDatabase.add(new Vault(file));
            }
//            vaultDatabase.add(new Vault(new File("C:\\Users\\Administrator\\Desktop\\Test\\AtomServerX\\vault\\Sample1")));

        } else {
            sayInfo(State.ERROR, "Main", "No vault at all...");
            VAULT_FILE_DIR.mkdirs();
            System.exit(-1);
        }
    }

    private static void checkARGS(String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "--no-color" -> loggist.setNoColor();
                case "--debug" -> IS_DEBUG_MODE = true;
            }
        }
    }

    public static void main(String[] args) {

        AtomServerX.initStructure();
        AtomServerX.checkARGS(args);

        sayInfo("-----------------------------------------------------");
        sayInfo("""

                   _____                                    \s
                  / ____|                                   \s
                 | |        ___   _ __    ___   __  __   ___\s
                 | |       / _ \\ | '__|  / _ \\  \\ \\/ /  / _ \\
                 | |____  |  __/ | |    | (_) |  >  <  |  __/
                  \\_____|  \\___| |_|     \\___/  /_/\\_\\  \\___|
                                                            \s
                                                             \
                """);


        sayInfo("Current log file : " + loggist.getLogFile().getAbsolutePath());
        sayInfo("LOCAL_DOMAIN_NAME: " + LOCAL_DOMAIN_NAME);
        sayInfo("Listen HOST_CONNECT_PORT on " + HOST_CONNECT_PORT);
        sayInfo("Listen HOST_HOOK_PORT on " + HOST_HOOK_PORT);
        sayInfo("Listen WINDOWS_UPDATE_PORT on " + WINDOWS_UPDATE_PORT);
        sayInfo("Listen LINUX_UPDATE_PORT on " + LINUX_UPDATE_PORT);
        sayInfo("Support client versions: " + EXPECTED_CLIENT_VERSION);


        AdminThread.startThread();//Not completed yet!
        CheckUpdateThread.startThread();

        while (true) {
            try {
                //listen for host client,and check is ban and available
                Socket hostServerHook = hostServerHookServerSocket.accept();
                if (IPChecker.exec(hostServerHook, IPChecker.CHECK_IS_BAN)) {
                    hostServerHook.close();
                    continue;
                }
                sayInfo("HostClient on " + hostServerHook.getInetAddress() + ":" + hostServerHook.getPort() + " try to connect !");
                HostClient hostClient;
                try {
                    hostClient = new HostClient(hostServerHook);
                } catch (IllegalConnectionException e) {
                    continue;
                }

                HostClient finalHostClient = hostClient;//temp use
                new Thread(() -> {
                    try {
                        //get and check host client property
                        Object[] obj = AtomServerX.checkHostClientVersionAndKeyAndLang(finalHostClient);
                        sayInfo("HostClient on " + hostServerHook.getInetAddress() + ":" + hostServerHook.getPort() + " register successfully!");
                        finalHostClient.enableCheckAliveThread();
                        availableHostClient.add(finalHostClient);

                        //generate them into pieces for use
                        finalHostClient.setVault((Vault) obj[0]);
                        finalHostClient.setLangData((LanguageData) obj[1]);

                        //arrange port if no specific , or give the chosen one
                        int port;
                        if (finalHostClient.getVault().getPort() != -1) {
                            port = finalHostClient.getVault().getPort();
                        } else {
                            port = AtomServerX.getCurrentAvailableOutPort();
                            if (port == -1) {
                                NoMorePortException.throwException();
                            }
                        }
                        finalHostClient.setClientServerSocket(new ServerSocket(port));
                        finalHostClient.setOutPort(port);

                        sendCommand(finalHostClient, String.valueOf(port));//tell the host client remote out port
                        //tell the message to the host client
                        sendStr(finalHostClient, finalHostClient.getLangData().THIS_ACCESS_CODE_HAVE + finalHostClient.getVault().getRate() + finalHostClient.getLangData().MB_IF_FLOW_LEFT);
                        sendStr(finalHostClient, finalHostClient.getLangData().EXPIRE_AT + finalHostClient.getVault().getEndDate());
                        sendStr(finalHostClient, finalHostClient.getLangData().USE_THE_ADDRESS + LOCAL_DOMAIN_NAME + ":" + port + finalHostClient.getLangData().TO_START_UP_CONNECTION);//send remote connect address
                        //print the property into the console
                        sayInfo("Assigned connection address: " + LOCAL_DOMAIN_NAME + ":" + port);


                        new Thread(() -> {
                            inner:
                            while (true) {

                                Socket client;
                                Socket host;

                                try {
                                    if (finalHostClient.getClientServerSocket().isClosed()) {
                                        break;//msg is print to the console at CheckAliveThread ! then it die
                                    } else {
                                        client = finalHostClient.getClientServerSocket().accept();
                                    }
                                } catch (IOException e) {
                                    if (IS_DEBUG_MODE) {
                                        e.printStackTrace();
                                    }
                                    continue;
                                }

                                try {//send ":>sendSocket;{clientAddr}" str to tell host client to connect
                                    //example ":>sendSocket;cha.ceron.fun:50001"
                                    sendCommand(finalHostClient, "sendSocket" + ";" + getInternetAddressAndPort(client));
                                } catch (Exception e) {
                                    sayHostClientDiscInfo(finalHostClient, "Main");
                                    closeSocket(hostServerHook);
                                    closeSocket(client);
                                    break;//break inner because host server hook is destroyed.
                                }

                                HostSign hostSign;
                                try {
                                    hostSign = TransferSocketAdapter.getThisHostClientHostSign(finalHostClient.getOutPort());
                                    host = hostSign.host();
                                } catch (IOException e) {//if host client timeout
                                    sayClientSuccConnecToChaSerButHostClientTimeOut(finalHostClient);
                                    sayInfo("Killing client's side connection: " + getInternetAddressAndPort(client));
                                    closeSocket(client);
                                    continue;
                                }
                                sayClientConnectBuildUpInfo(finalHostClient, client);//say connection build up info

                                //start real transfer service
                                Transformer.startThread(finalHostClient, host, client, hostSign);
                            }
                        }).start();
                    } catch (UnSupportHostVersionException | IndexOutOfBoundsException | IOException |
                             NoMorePortException |
                             AlreadyBlindPortException | UnRecognizeVaultException e) {
                        // exception class will auto say OTHER info ! Just do things.
                        sayHostClientDiscInfo(finalHostClient, "Main");
                        closeSocket(hostServerHook);
                    } catch (SlientException ignore) {
                    }
                }).start();
            } catch (IOException e) {
                sayInfo(State.INFO, "Main", "A host client try to connect but fail .");
            }
        }

    }


    public static void sayInfo(String str) {
        loggist.say(new State(State.INFO, "Main", str));
    }

    public static void sayInfo(int type, String subject, String str) {
        loggist.say(new State(type, subject, str));
    }

    private static int getCurrentAvailableOutPort() throws IOException {
        for (int i = START_PORT; i <= END_PORT; i++) {
            try {
                ServerSocket serverSocket = new ServerSocket(i);
                serverSocket.close();
                return i;
            } catch (Exception ignore) {
            }
        }
        return -1;
    }

    private static Object[] checkHostClientVersionAndKeyAndLang(HostClient hostClient) throws IOException, UnSupportHostVersionException, UnRecognizeVaultException, AlreadyBlindPortException, IndexOutOfBoundsException, SlientException {
        if (hostClient.getReader() == null) {
            SlientException.throwException();
        }
        String hostClientInfo = receiveStr(hostClient);//host client property in one line

        if (hostClientInfo == null) {
            UnSupportHostVersionException.throwException("_NULL_", hostClient);
        }

        assert hostClientInfo != null;
        String[] info = hostClientInfo.split(";");//make them into pieces for use

        if (info.length != 3) {
            UnSupportHostVersionException.throwException("_NULL_", hostClient);
        }

        // zh;version;key
        LanguageData languageData;
        if (info[0].equals("zh")) {
            languageData = LanguageData.getChineseLanguage();
        } else {
            languageData = new LanguageData();
        }

        boolean isAvailVersion = availableVersions.contains(info[1]);
        if (!isAvailVersion) {
            sendStr(hostClient, languageData.UNSUPPORTED_VERSION_MSG + EXPECTED_CLIENT_VERSION);
            hostClient.close();
            UnSupportHostVersionException.throwException(info[1], hostClient);
        }

        Vault currentVault = AtomServerX.getVaultOnVaultDatabase(info[2]);
        if (currentVault == null) {
            sendStr(hostClient, languageData.ACCESS_DENIED_FORCE_EXITING);
            hostClient.close();
            UnRecognizeVaultException.throwException(info[2]);
        }

        assert currentVault != null;
        if (currentVault.getPort() != -1) {//check port on vault file!
            try {

                ServerSocket serverSocket = new ServerSocket(currentVault.getPort());//check is not occupied
                serverSocket.close();

            } catch (IOException e) {// if is blind
                sendStr(hostClient, languageData.THE_PORT_HAS_ALREADY_BLIND);
                AlreadyBlindPortException.throwException(currentVault.getPort());
            }
        }

        //if nothing is bad
        sendStr(hostClient, languageData.CONNECTION_BUILD_UP_SUCCESSFULLY);
        return new Object[]{currentVault, languageData};
    }

    public static Vault getVaultOnVaultDatabase(String key) {
        for (Vault vault : vaultDatabase) {
            if (key.equals(vault.getName())) {// vault file does not have suffix
                return vault;
            }
        }
        return null;
    }

}
