package atomserverx;

import asia.ceroxe.time.Time;
import atomserverx.exceptions.NoMoreNetworkFlowException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Vault {
    private double rate;//mb
    private String endDate;//2023/3/2 13:33;
    private File vaultFile;
    private int port = -1;

    public Vault(File vaultFile) {

        try {
            this.vaultFile = vaultFile;
            readAndSetElementFromFile(vaultFile);
        } catch (Exception e) {
            e.printStackTrace();
            vaultFile.delete();
        }
    }

    public static boolean isOutOfDate(String endTime) {
        //2023/3/2 13:33
        String[] elements = endTime.split(" ");
        String[] date = elements[0].split("/");
        String[] time = elements[1].split(":");

        if (Time.getCurrentYear() < Integer.parseInt(date[0])) {
            return false;

        } else if (Time.getCurrentYear() > Integer.parseInt(date[0])) {
            return true;
        } else {
            if (Time.getCurrentMonth() > Integer.parseInt(date[1])) {
                return true;
            } else if (Time.getCurrentMonth() < Integer.parseInt(date[1])) {
                return false;
            } else {
                if (Time.getCurrentDay() > Integer.parseInt(date[2])) {
                    return true;
                } else if (Time.getCurrentDay() < Integer.parseInt(date[2])) {
                    return false;
                } else {
                    if (Time.getCurrentHour() > Integer.parseInt(time[0])) {
                        return true;
                    } else if (Time.getCurrentHour() < Integer.parseInt(time[0])) {
                        return false;
                    } else {
                        if (Time.getCurrentMinutes() > Integer.parseInt(time[1])) {
                            return true;
                        } else return Time.getCurrentMinutes() >= Integer.parseInt(time[1]);
                    }
                }
            }
        }
    }

    public static void removeVaultOnAll(Vault vault) {
        AtomServerX.vaultDatabase.remove(vault);
    }

    public void readAndSetElementFromFile(File vaultFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(vaultFile));
        rate = Double.parseDouble(bufferedReader.readLine());
        endDate = bufferedReader.readLine();
        try {
            port = Integer.parseInt(bufferedReader.readLine());
        } catch (Exception e) {
            port = -1;
        }
        bufferedReader.close();
    }

    public String getEndDate() {
        return endDate;
    }

    public boolean isOutOfDate() {
        return Vault.isOutOfDate(endDate);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port > 0 && port <= 65535) {
            this.port = port;
        }
    }

    public double getRate() {
        return rate;
    }

    public String getName() {
        return this.vaultFile.getName();
    }

    public File getFile() {
        return vaultFile;
    }

    public void addMib(double mib) {
        rate = rate + mib;
    }

    public synchronized void mineMib(double mib) throws NoMoreNetworkFlowException {
        if (rate > 0) {
            rate = rate - mib;
        } else {
            if (vaultFile.exists()) {
                vaultFile.delete();
                //the exception will auto say
            }
            NoMoreNetworkFlowException.throwException(this.vaultFile.getName());
        }

    }

    public void save() {
        try {
            if ((!Vault.isOutOfDate(endDate)) && rate > 0) {
                vaultFile.delete();
                vaultFile.createNewFile();
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(vaultFile, StandardCharsets.UTF_8));
                bufferedWriter.write(String.valueOf(rate));
                bufferedWriter.newLine();
                bufferedWriter.write(endDate);
                if (port != -1) {
                    bufferedWriter.newLine();
                    bufferedWriter.write(String.valueOf(port));
                }
                bufferedWriter.close();
            } else {
                if (vaultFile.exists()) {
                    vaultFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
