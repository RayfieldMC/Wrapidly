/*
MIT License

Copyright (c) 2025 Rarfield

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Launcher {
    public static void main(String[] args) {
        try {
            // Ensure wrapper.properties exists
            File propsFile = new File("wrapper.properties");
            if (!propsFile.exists()) {
                try (FileWriter writer = new FileWriter(propsFile)) {
                    writer.write("# JVM command line to launch your server\n");
                    writer.write("jvm=java -jar server.jar\n\n");

                    writer.write("# Discord webhook URL to send server start/stop messages (optional)\n");
                    writer.write("webhook=\n\n");

                    writer.write("# Command remapping (optional)\n");
                    writer.write("# To remap a command, use this format:\n");
                    writer.write("# remap.<original_command>=<new_command>\n");
                    writer.write("#\n");
                    writer.write("# Example remaps (uncomment to use):\n");
                    writer.write("# remap.stop=end\n");
                    writer.write("# remap.restart=restartwrapper\n");
                    writer.write("# remap.shutdown=end\n");

                    System.out.println("[Launcher] Created wrapper.properties with default values.");
                }
            }

            // Load wrapper.properties
            Properties props = new Properties();
            try (FileReader reader = new FileReader(propsFile)) {
                props.load(reader);
            }

            // Parse jvm command
            String jvmCommand = props.getProperty("jvm", "java -jar server.jar").trim();
            List<String> command = new ArrayList<>(Arrays.asList(jvmCommand.split(" ")));

            // Parse webhook
            String webhookUrl = props.getProperty("webhook", "").trim();

            // Parse remap
            Map<String, String> remap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("remap.")) {
                    String originalCommand = key.substring(6);
                    String mappedCommand = props.getProperty(key).trim();
                    if (!originalCommand.isEmpty() && !mappedCommand.isEmpty()) {
                        remap.put(originalCommand, mappedCommand);
                    }
                }
            }

            // Send "Server started" webhook if webhook is set
            if (!webhookUrl.isEmpty()) {
                sendDiscordWebhook(webhookUrl, "🟢 **Server Started**", "Your Minecraft server has started successfully.");
            }

            // Build and start the process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

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

            // Input thread with remap
            Thread inputThread = new Thread(() -> {
                try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    String commandLine;
                    while ((commandLine = consoleReader.readLine()) != null) {
                        String actualCommand = remap.getOrDefault(commandLine, commandLine);
                        writer.write(actualCommand);
                        writer.newLine();
                        writer.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            inputThread.start();

            // Wait for process to exit
            int exitCode = process.waitFor();
            System.out.println("[Launcher] The server exited with code " + exitCode);

            // Send "Server stopped" webhook if webhook is set
            if (!webhookUrl.isEmpty()) {
                sendDiscordWebhook(webhookUrl, "🔴 **Server Stopped**", "Your Minecraft server has stopped. Exit code: " + exitCode);
            }

            // Clean up
            outputThread.interrupt();
            inputThread.interrupt();
            process.destroy();
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Send webhook to Discord
    private static void sendDiscordWebhook(String webhookUrl, String title, String description) {
        try {
            String jsonPayload = String.format(
                "{\"embeds\": [{\"title\": \"%s\", \"description\": \"%s\", \"color\": %d}]}",
                escapeJson(title),
                escapeJson(description),
                3066993 // Green by default
            );

            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("[Launcher] Webhook sent successfully.");
            } else {
                System.out.println("[Launcher] Failed to send webhook. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (Exception e) {
            System.out.println("[Launcher] Failed to send webhook.");
            e.printStackTrace();
        }
    }

    // Escape JSON string
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
