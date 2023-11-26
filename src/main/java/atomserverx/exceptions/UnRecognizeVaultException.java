package atomserverx.exceptions;

import atomserverx.AtomServerX;

public class UnRecognizeVaultException extends Exception {
    private UnRecognizeVaultException(String msg) {
        super(msg);
    }

    public static void throwException(String vaultCode) throws UnRecognizeVaultException {
        String str = "The access code " + vaultCode + " could not find in vault dir.";
        AtomServerX.sayInfo(str);
        throw new UnRecognizeVaultException(str);
    }
}
