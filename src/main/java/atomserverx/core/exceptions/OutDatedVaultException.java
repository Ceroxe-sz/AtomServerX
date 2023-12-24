package atomserverx.core.exceptions;

import atomserverx.core.Vault;

public class OutDatedVaultException extends Exception {
    private OutDatedVaultException(String msg) {
        super(msg);
    }

    public static void throwException(Vault vault) throws OutDatedVaultException {
        throw new OutDatedVaultException("The vault " + vault.getName() + " are out of date.");
    }
}
