package atomserverx.core.exceptions;

import atomserverx.AtomServerX;

public class NoMoreNetworkFlowException extends Exception {
    private NoMoreNetworkFlowException(String msg) {
        super(msg);
    }

    public static void throwException(String accessCode) throws NoMoreNetworkFlowException {
        String str = "The access code " + accessCode + " network flow now is zero !";
        AtomServerX.sayInfo(str);
        throw new NoMoreNetworkFlowException(str);
    }
}
