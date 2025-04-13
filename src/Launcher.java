import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Ensure the launcher properties file exists
            File propsFile = new File("launcher.properties");
            if (!propsFile.exists()) {
                try (FileWriter writer = new FileWriter(propsFile)) {
                    writer.write("java -javaagent:authlibinjector.jar=ely.by -jar server.jar");
                    System.out.println("[Launcher] Created launcher.properties with default JVM arguments");
                }
            }

            // Read the server jar path and JVM arguments from launcher.properties
            BufferedReader br = new BufferedReader(new FileReader("launcher.properties"));
            String serverJar = br.readLine().trim();
            br.close();

            // Split the command string into a list of arguments
            List<String> command = new ArrayList<>(Arrays.asList(serverJar.split(" ")));

            // Build and start the process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Merge stdout and stderr
            Process process = pb.start();

            // Output thread to capture Minecraft server's console output
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Input thread to send commands to the server
            Thread inputThread = new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    String commandLine;
                    while ((commandLine = consoleReader.readLine()) != null) {
                        writer.write(commandLine);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            inputThread.start();

            // Wait for the server process to exit, and then terminate the launcher
            int exitCode = process.waitFor();
            System.out.println("[Launcher] The server exited with code " + exitCode);

            // If the server exited with code 0, also terminate the launcher
            if (exitCode == 0) {
                System.out.println("[Launcher] Server stopped successfully. Shutting down the launcher...");
                System.exit(0); // This will terminate the launcher process
            }

            // Clean up threads
            outputThread.interrupt();
            inputThread.interrupt();
            process.destroy();  // Explicitly destroy the process if needed

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
