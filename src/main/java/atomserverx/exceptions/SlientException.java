package atomserverx.exceptions;

public class SlientException extends Exception {
    private SlientException() {
        super();
    }

    public static void throwException() throws SlientException {
        throw new SlientException();
    }
}
