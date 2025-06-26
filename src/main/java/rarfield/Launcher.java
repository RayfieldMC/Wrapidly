package rarfield;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.*;

public class Launcher {
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        logHeader("WRAPIDLY LAUNCHER v1.0.6");

        File configFile = new File("wrapper.yml");
        Map<String, Object> config;

        try {
            if (!configFile.exists()) {
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("""
                            # Wrapidly Config
                            jvm: java -jar server.jar
                            webhook: ""
                            remap:
                              # stop: end
                            macros:
                              # restartserver: |
                                # say Restarting...
                                # stop
                            autoRestart: true
                            requireHealthyStartup: true
                            preStart:
                              - echo Hello
                            """);
                }
                logInfo("Created default wrapper.yml.");
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            config = yaml.load(new FileInputStream(configFile));

        } catch (Exception e) {
            logError("Failed to load wrapper.yml: " + e.getMessage());
            return;
        }

        String jvmCommand = ((String) config.getOrDefault("jvm", "java -jar server.jar")).trim();
        String webhookUrl = ((String) config.getOrDefault("webhook", "")).trim();
        boolean autoRestart = Boolean.TRUE.equals(config.get("autoRestart"));
        boolean requireHealthy = Boolean.TRUE.equals(config.get("requireHealthyStartup"));

        Map<String, String> remap = new HashMap<>();
        if (config.get("remap") instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                remap.put(String.valueOf(key), String.valueOf(map.get(key)));
            }
        }

        Map<String, List<String>> macros = new HashMap<>();
        if (config.get("macros") instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                String name = String.valueOf(key);
                String value = String.valueOf(map.get(key));
                macros.put(name, Arrays.asList(value.split("\\r?\\n")));
            }
        }

        if (!runPreStartupCheck(jvmCommand, webhookUrl) && requireHealthy) {
            logError("Pre-startup check failed. Launch cancelled.");
            sendWebhook(webhookUrl, "❌ **Startup check failed. Server not launched.**", 0xff0000);
            return;
        }

        runPreStartCommands(config);

        do {
            logInfo("Launching with command: " + jvmCommand);
            sendWebhook(webhookUrl, "🚀 Server starting...", 0x3aa856);

            try {
                ProcessBuilder pb = new ProcessBuilder(jvmCommand.split(" "));
                pb.redirectErrorStream(true);
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
                });

                Thread inputThread = new Thread(() -> {
                    try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                        String commandLine;
                        while ((commandLine = consoleReader.readLine()) != null) {
                            if (commandLine.startsWith("!")) {
                                String macroName = commandLine.substring(1);
                                List<String> macroCommands = macros.get(macroName);
                                if (macroCommands != null) {
                                    logInfo("Running macro: !" + macroName);
                                    for (String cmd : macroCommands) {
                                        writer.write(cmd);
                                        writer.newLine();
                                    }
                                    writer.flush();
                                } else {
                                    logError("Unknown macro: !" + macroName);
                                }
                                continue;
                            }

                            if (remap.containsKey(commandLine)) {
                                String remapped = remap.get(commandLine);
                                logInfo("Remapped: " + commandLine + " → " + remapped);
                                commandLine = remapped;
                            }

                            writer.write(commandLine);
                            writer.newLine();
                            writer.flush();
                        }
                    } catch (IOException e) {
                        logError("Error sending input: " + e.getMessage());
                    }
                });

                outputThread.start();
                inputThread.start();

                int exitCode = process.waitFor();
                logInfo("Server exited with code " + exitCode);
                sendWebhook(webhookUrl, "🛑 Server stopped with code `" + exitCode + "`", 0xc0392b);

                outputThread.interrupt();
                inputThread.interrupt();

                if (!autoRestart || exitCode == 0) break;
                logWarn("Server crashed. Restarting...");

            } catch (Exception e) {
                logError("Failed to launch server: " + e.getMessage());
                break;
            }
        } while (true);
    }

    private static void logHeader(String msg) {
        String border = "+------------------------------+";
        String center = "|  " + msg;
        while (center.length() < border.length() - 1) center += " ";
        center += "|";

        System.out.println(CYAN);
        System.out.println(border);
        System.out.println(center);
        System.out.println(border + RESET);
    }

    private static void logInfo(String msg) {
        System.out.println(CYAN + "[INFO] " + RESET + msg);
    }

    private static void logWarn(String msg) {
        System.out.println(YELLOW + "[WARN] " + RESET + msg);
    }

    private static void logError(String msg) {
        System.err.println(RED + "[ERROR] " + RESET + msg);
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

            conn.getResponseCode();
        } catch (Exception e) {
            logError("Webhook failed: " + e.getMessage());
        }
    }

    private static boolean runPreStartupCheck(String jvmCommand, String webhookUrl) {
        logHeader("PRE-STARTUP CHECK");
        boolean allGood = true;

        String jarFile = Arrays.stream(jvmCommand.split(" "))
                .filter(s -> s.endsWith(".jar"))
                .findFirst().orElse("server.jar");

        if (!new File(jarFile).exists()) {
            logError("[X] Missing " + jarFile);
            allGood = false;
        } else logInfo("[OK] Found " + jarFile);

        File eula = new File("eula.txt");
        if (!eula.exists()) {
            logWarn("[!] Missing eula.txt");
            allGood = false;
        } else {
            try {
                String content = java.nio.file.Files.readString(eula.toPath());
                if (!content.contains("eula=true")) {
                    logWarn("[!] eula.txt not accepted");
                    allGood = false;
                } else logInfo("[OK] eula.txt accepted");
            } catch (IOException e) {
                logError("[X] Could not read eula.txt");
                allGood = false;
            }
        }

        if (new File("plugins").exists() || new File("mods").exists())
            logInfo("[OK] Server content folder found");
        else
            logWarn("[!] No plugins/ or mods/ folder");

        try (ServerSocket socket = new ServerSocket(25565)) {
            logInfo("[OK] Port 25565 available");
        } catch (IOException e) {
            logWarn("[!] Port 25565 in use");
            allGood = false;
        }

        return allGood;
    }

    private static void runPreStartCommands(Map<String, Object> config) {
        Object obj = config.get("preStart");
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                try {
                    String cmd = String.valueOf(o);
                    logInfo("Running: " + cmd);
                    Process p = new ProcessBuilder(cmd.split(" ")).inheritIO().start();
                    p.waitFor();
                } catch (Exception e) {
                    logError("Pre-start command failed: " + e.getMessage());
                }
            }
        }
    }
}
