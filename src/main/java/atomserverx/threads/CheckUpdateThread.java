package atomserverx.threads;

import plethora.management.bufferedFile.BufferedFile;
import atomserverx.IPChecker;
import plethora.utils.StringUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static atomserverx.AtomServerX.*;
import static atomserverx.SocketOperator.closeSocket;
import static atomserverx.SocketOperator.getInternetAddressAndPort;

public class CheckUpdateThread implements Runnable {
    public static int WINDOWS_UPDATE_PORT = 803;
    public static int LINUX_UPDATE_PORT = 804;

    public static void startThread() {
        Thread t = new Thread(new CheckUpdateThread());
        t.start();
    }

    private static void enable(String dirPath) {
        new Thread(() -> {
            BufferedFile destDir = new BufferedFile(dirPath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            try {
                BufferedFile destFile = destDir.getAllFiles().get(0);

                ServerSocket serverSocket;
                if (destFile.getName().endsWith(".exe")) {
                    serverSocket = new ServerSocket(WINDOWS_UPDATE_PORT);
                } else {
                    serverSocket = new ServerSocket(LINUX_UPDATE_PORT);
                }

                while (true) {
                    Socket client = serverSocket.accept();
                    if (IPChecker.exec(client, IPChecker.CHECK_IS_BAN)) {
                        client.close();
                        continue;
                    }

                    if (destFile.getName().endsWith(".exe")) {
                        sayInfo("A host client from " + client.getInetAddress() + " try to download an EXE update.");
                    } else {
                        sayInfo("A host client from " + client.getInetAddress() + " try to download an JAR update.");
                    }

                    new Thread(() -> {
                        try {
                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(client.getOutputStream());
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(destFile));

                            byte[] data = new byte[500000];//0.5mb
                            int len;
                            while ((len = bufferedInputStream.read(data)) != -1) {
                                bufferedOutputStream.write(data, 0, len);
                                bufferedOutputStream.flush();
                            }
                            bufferedOutputStream.close();//socket will close
                            bufferedInputStream.close();
                            client.close();

                            if (destFile.getName().endsWith(".exe")) {
                                sayInfo("A host client from " + client.getInetAddress() + " download an EXE update success !");
                            } else {
                                sayInfo("A host client from " + client.getInetAddress() + " download an JAR update success !");
                            }
                        } catch (Exception e) {
                            sayInfo("Fail to write data to " + getInternetAddressAndPort(client) + " BAN IT!!!!");
                            IPChecker.exec(client, IPChecker.DO_BAN);
                            closeSocket(client);
                        }
                    }).start();
                }
            } catch (IOException e) {
                if (IS_DEBUG_MODE) {
                    String exceptionMsg= StringUtils.getExceptionMsg(e);
                    System.out.println(exceptionMsg);
                    loggist.write(exceptionMsg);
                }
                System.exit(-1);
            } catch (IndexOutOfBoundsException e) {
                if (dirPath.contains("exe")) {
                    sayInfo("No EXE file in exe directory , close thread...");
                } else {
                    sayInfo("No JAR file in jar directory , close thread...");
                }
            }
        }).start();
    }

    @Override
    public void run() {
        CheckUpdateThread.enable(System.getProperty("user.dir") + File.separator + "client" + File.separator + "exe");
        CheckUpdateThread.enable(System.getProperty("user.dir") + File.separator + "client" + File.separator + "jar");
    }
}
