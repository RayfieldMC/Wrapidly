import java.io.*;
import java.util.*;

public class Launcher {
    public static void main(String[] args) throws Exception {
        // Load settings
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("settings.properties")) {
            props.load(input);
        }

        // Read command
        String command = props.getProperty("launch");
        if (command == null || command.isBlank()) {
            System.out.println("No launch command in settings.properties");
            return;
        }

        // Build the process
        List<String> cmd = Arrays.asList(command.split(" "));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO(); // or redirectOutput to log files if needed
        Process p = pb.start();

        // Wait so the host doesn’t think it crashed
        p.waitFor();
    }
}
