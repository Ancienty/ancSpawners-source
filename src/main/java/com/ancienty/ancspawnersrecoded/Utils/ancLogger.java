package com.ancienty.ancspawnersrecoded.Utils;

import com.ancienty.ancspawnersrecoded.Main;

import java.io.*;

public class ancLogger {

    private File logFile;

    public void createLogFile() {
        File file = new File(Main.getPlugin().getDataFolder(), File.separator + "logs" + File.separator + "errors.txt");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs(); // Ensure the logs directory exists
                file.createNewFile();
            }
            logFile = file;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeError(String message) {
        if (logFile == null) {
            createLogFile();
        }
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
