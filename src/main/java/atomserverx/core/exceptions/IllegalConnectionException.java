package atomserverx.core.exceptions;

import atomserverx.core.IPChecker;
import atomserverx.core.SocketOperator;

import java.net.Socket;

import static atomserverx.AtomServerX.sayInfo;

public class IllegalConnectionException extends Exception {
    private IllegalConnectionException(String msg) {
        super(msg);
    }

    public static void throwException(Socket socket) throws IllegalConnectionException {
        if (IPChecker.ENABLE_BAN) {
            sayInfo("The " + SocketOperator.getIP(socket) + " attempted an illegal connection,BAN IT!!!");
            IPChecker.exec(socket, IPChecker.DO_BAN);
        } else {
            sayInfo("The " + SocketOperator.getIP(socket) + " attempted an illegal connection,close it...");
        }
    }
}
