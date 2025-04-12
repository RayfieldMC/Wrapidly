import java.io.*;

public class Launcher {
    public static void main(String[] args) {
        File settingsFile = new File("settings.properties");

        
        if (!settingsFile.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(settingsFile))) {
                writer.println("-javaagent:authlibinjector.jar=ely.by -jar server.jar");
                System.out.println("[Launcher] Created settings.properties with default arguments.");
            } catch (IOException e) {
                System.err.println("[Launcher] Failed to create settings.properties: " + e.getMessage());
                return;
            }
        }

        
        String launchArgs;
        try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
            launchArgs = reader.readLine();
            if (launchArgs == null || launchArgs.isBlank()) {
                System.err.println("[Launcher] settings.properties is empty. Add your JVM args!");
                return;
            }
        } catch (IOException e) {
            System.err.println("[Launcher] Error reading settings.properties: " + e.getMessage());
            return;
        }

        
        String[] argsFromFile = launchArgs.trim().split("\\s+");
        String[] command = new String[argsFromFile.length + 1];
        command[0] = "java";
        System.arraycopy(argsFromFile, 0, command, 1, argsFromFile.length);

        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            
            try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = output.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (IOException e) {
            System.err.println("[Launcher] Failed to launch server: " + e.getMessage());
        }
    }
}
