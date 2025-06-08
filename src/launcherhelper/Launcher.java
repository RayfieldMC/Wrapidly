package launcherhelper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Ensure launcher.properties exists
            File propsFile = new File("launcher.properties");
            if (!propsFile.exists()) {
                try (FileWriter writer = new FileWriter(propsFile)) {
                    writer.write("java -javaagent:authlibinjector.jar=ely.by -jar server.jar");
                    System.out.println("[Launcher] Created launcher.properties with default JVM arguments");
                }
            }

            // Ensure launcher-webhook.properties exists
            Properties webhookProps = new Properties();
            File webhookFile = new File("launcher-webhook.properties");
            if (!webhookFile.exists()) {
                try (FileWriter writer = new FileWriter(webhookFile)) {
                    writer.write("webhookUrl=");
                    System.out.println("[Launcher] Created launcher-webhook.properties (please add your Discord webhook URL)");
                }
            }

            // Load webhook URL
            webhookProps.load(new FileReader("launcher-webhook.properties"));
            String webhookUrl = webhookProps.getProperty("webhookUrl", "").trim();

            // Load remap.properties if exists
            Map<String, String> commandRemap = new HashMap<>();
            File remapFile = new File("remap.properties");
            if (remapFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(remapFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            commandRemap.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                    System.out.println("[Launcher] Loaded " + commandRemap.size() + " command remaps.");
                } catch (Exception e) {
                    System.out.println("[Launcher] Failed to load remap.properties: " + e.getMessage());
                }
            } else {
                System.out.println("[Launcher] No remap.properties found. Command remapping disabled.");
            }

            // Read launcher.properties for server command
            BufferedReader br = new BufferedReader(new FileReader("launcher.properties"));
            String serverCommand = br.readLine().trim();
            br.close();

            List<String> command = new ArrayList<>(Arrays.asList(serverCommand.split(" ")));

            // Build and start process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Send "server starting" webhook
            if (!webhookUrl.isEmpty()) {
                sendDiscordWebhook(webhookUrl, "🟢 Server is starting...");
            }

            // Output thread
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

            // Input thread
            Thread inputThread = new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    String commandLine;
                    while ((commandLine = consoleReader.readLine()) != null) {
                        String mappedCommand = commandRemap.getOrDefault(commandLine, commandLine);
                        if (!mappedCommand.equals(commandLine)) {
                            System.out.println("[Launcher] Remapped command '" + commandLine + "' -> '" + mappedCommand + "'");
                        }
                        writer.write(mappedCommand);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            inputThread.start();

            int exitCode = process.waitFor();
            System.out.println("[Launcher] The server exited with code " + exitCode);

            // Send "server stopped" webhook
            if (!webhookUrl.isEmpty()) {
                sendDiscordWebhook(webhookUrl, "🔴 Server stopped with code " + exitCode);
            }

            if (exitCode == 0) {
                System.out.println("[Launcher] Server stopped successfully. Shutting down the launcher...");
            } else {
                System.out.println("[Launcher] Server stopped faulty. Shutting down the launcher...");
            }

            outputThread.interrupt();
            inputThread.interrupt();
            process.destroy();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendDiscordWebhook(String webhookUrl, String message) {
        try {
            String jsonPayload = "{"
                + "\"embeds\": [{"
                + "\"title\": \"" + message + "\","
                + "\"color\": 65280"
                + "}]"
                + "}";

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204 && responseCode != 200) {
                System.out.println("[Launcher] Failed to send webhook, HTTP code: " + responseCode);
            } else {
                System.out.println("[Launcher] Webhook sent successfully.");
            }

        } catch (Exception e) {
            System.out.println("[Launcher] Failed to send webhook: " + e.getMessage());
        }
    }
}
