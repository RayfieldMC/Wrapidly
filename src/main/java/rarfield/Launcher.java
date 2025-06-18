package rarfield;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Launcher {
    public static void main(String[] args) {
        logInfo("Wrapidly v1.0.5 starting up...");

        File configFile = new File("wrapper.yml");
        Map<String, Object> config = new HashMap<>();

        try {
            if (!configFile.exists()) {
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("""
                            # Wrapidly Config
                            jvm: java -jar server.jar

                            webhook: ""  # Optional: Discord webhook URL

                            # You can remap commands you type in the console
                            # Example: typing 'stop' will actually send 'end' to the server
                            # Example commands are given down. Just uncomment them
                            remap:
                              # stop: end
                              # restart: stop
                            """);
                }
                logInfo("Created default wrapper.yml.");
            }

            // Load YAML config
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            config = yaml.load(new FileInputStream(configFile));

        } catch (Exception e) {
            logError("Failed to load wrapper.yml: " + e.getMessage());
            return;
        }

        String jvmCommand = ((String) config.getOrDefault("jvm", "java -jar server.jar")).trim();
        String webhookUrl = ((String) config.getOrDefault("webhook", "")).trim();

        Map<String, String> remap = new HashMap<>();
        Object remapObj = config.get("remap");
        if (remapObj instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                remap.put(String.valueOf(key), String.valueOf(map.get(key)));
            }
        }

        logInfo("Launching with command: " + jvmCommand);
        sendWebhook(webhookUrl, "Server starting...", 0x3aa856); // Green

        List<String> command = Arrays.asList(jvmCommand.split(" "));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    logError("Error reading output: " + e.getMessage());
                }
            }, "OutputThread");

            Thread inputThread = new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    String commandLine;
                    while ((commandLine = consoleReader.readLine()) != null) {
                        String originalCommand = commandLine.trim();
                        if (remap.containsKey(originalCommand)) {
                            String newCommand = remap.get(originalCommand);
                            logInfo("Remapped command: " + originalCommand + " → " + newCommand);
                            commandLine = newCommand;
                        }
                        writer.write(commandLine);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    logError("Error sending input: " + e.getMessage());
                }
            }, "InputThread");

            outputThread.start();
            inputThread.start();

            // Wait for server process to exit
            int exitCode = process.waitFor();
            logInfo("Server exited with code " + exitCode);

            sendWebhook(webhookUrl, "Server stopped with code `" + exitCode + "`", 0xc0392b); // Red

            // Interrupt threads
            outputThread.interrupt();
            inputThread.interrupt();

            // Cleanup process
            if (process.isAlive()) process.destroy();

            // Exit Wrapidly with same exit code as server
            System.exit(exitCode);

        } catch (Exception e) {
            logError("Failed to launch server: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void sendWebhook(String url, String msg, int color) {
        if (url == null || url.isEmpty()) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = String.format("""
                    {
                      "embeds": [{
                        "title": "%s",
                        "color": %d
                      }]
                    }
                    """, msg, color);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            conn.getResponseCode(); // Send the request

        } catch (Exception e) {
            logError("Failed to send webhook: " + e.getMessage());
        }
    }

    private static void logInfo(String msg) {
        System.out.println("\u001B[36m[Wrapidly]\u001B[0m " + msg); // Cyan
    }

    private static void logError(String msg) {
        System.err.println("\u001B[31m[Wrapidly ERROR]\u001B[0m " + msg); // Red
    }
}
