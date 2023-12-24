package atomserverx.core.exceptions;

import atomserverx.AtomServerX;

public class NoMorePortException extends Exception {
    private NoMorePortException(String msg) {
        super(msg);
    }

    public static void throwException() throws NoMorePortException {
        String str = "There are no more dynamic ports available";
        AtomServerX.sayInfo(str);
        throw new NoMorePortException(str);
    }
}
