package atomserverx.exceptions;

import atomserverx.AtomServerX;

public class AlreadyBlindPortException extends Exception {
    private AlreadyBlindPortException(int port) {
        super(String.valueOf(port));
    }

    public static void throwException(int port) throws AlreadyBlindPortException {
        AtomServerX.sayInfo("The port " + port + " has already blind !");
        throw new AlreadyBlindPortException(port);
    }
}
