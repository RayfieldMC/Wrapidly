package rarfield;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Launcher {

    public static void main(String[] args) {
        while (true) {
            LaunchResult result = launchOnce();

            if (result.exitCode == 0) {
                log("[OK] Server stopped normally. Exiting Wrapidly.");
                break;
            }

            if (!result.autoRestart) {
                log("[X] Server crashed and autoRestart is disabled. Exiting Wrapidly.");
                break;
            }

            log("[!] Server crashed. Restarting due to autoRestart = true.");
        }
    }

    private static LaunchResult launchOnce() {
        printBanner();

        File jar = new File("server.jar");
        if (!jar.exists()) {
            log("[X] server.jar not found.");
            return new LaunchResult(1, false);
        } else {
            log("[OK] server.jar found.");
        }

        File configFile = new File("wrapper.yml");
        Map<String, Object> config = new HashMap<>();

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
                              # reboot: |
                              #   say Rebooting...
                              #   save-all
                              #   stop

                            requireHealthyStartup: true
                            autoRestart: true

                            preStartCommands:
                              # - echo Preparing to launch...
                              # - mkdir logs
                            """);
                }
                log("[OK] Created default wrapper.yml.");
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            config = yaml.load(new FileInputStream(configFile));
        } catch (Exception e) {
            log("[X] Failed to load wrapper.yml: " + e.getMessage());
            return new LaunchResult(1, false);
        }

        String jvmCommand = ((String) config.getOrDefault("jvm", "java -jar server.jar")).trim();
        String webhook = ((String) config.getOrDefault("webhook", "")).trim();
        boolean autoRestart = Boolean.parseBoolean(String.valueOf(config.getOrDefault("autoRestart", "false")));
        boolean requireHealthyStartup = Boolean.parseBoolean(String.valueOf(config.getOrDefault("requireHealthyStartup", "false")));

        Map<String, String> remap = new HashMap<>();
        if (config.get("remap") instanceof Map<?, ?> map) {
            map.forEach((k, v) -> remap.put(String.valueOf(k), String.valueOf(v)));
        }

        Map<String, List<String>> macros = new HashMap<>();
        if (config.get("macros") instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                String[] lines = String.valueOf(map.get(key)).split("\\r?\\n");
                macros.put(String.valueOf(key), Arrays.asList(lines));
            }
        }

        List<String> preStartCommands = new ArrayList<>();
        if (config.get("preStartCommands") instanceof List<?> list) {
            for (Object o : list) preStartCommands.add(String.valueOf(o));
        }

        if (!preStartCommands.isEmpty()) {
            log("[●] Running pre-start commands...");
            for (String cmd : preStartCommands) {
                try {
                    log("    > " + cmd);
                    Process p = new ProcessBuilder(cmd.split(" ")).inheritIO().start();
                    p.waitFor();
                } catch (Exception e) {
                    log("[!] Failed: " + e.getMessage());
                }
            }
        }

        log("[●] Launching server with: " + jvmCommand);
        sendWebhook(webhook, "Server starting...", 0x3aa856);

        try {
            ProcessBuilder pb = new ProcessBuilder(jvmCommand.split(" "));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        if (requireHealthyStartup && line.toLowerCase().contains("exception")) {
                            log("[X] Detected error in startup!");
                            sendWebhook(webhook, "Startup error detected.", 0xff0000);
                        }
                    }
                } catch (IOException e) {
                    log("[!] Output error: " + e.getMessage());
                }
            });
            outputThread.setDaemon(true);

            Thread inputThread = new Thread(() -> {
                try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                    String input;
                    while ((input = console.readLine()) != null) {
                        if (remap.containsKey(input)) {
                            String newCmd = remap.get(input);
                            log("[!] Remap: " + input + " → " + newCmd);
                            input = newCmd;
                        }

                        if (macros.containsKey(input)) {
                            log("[!] Macro: " + input);
                            for (String macroCmd : macros.get(input)) {
                                writer.write(macroCmd);
                                writer.newLine();
                                writer.flush();
                            }
                        } else {
                            writer.write(input);
                            writer.newLine();
                            writer.flush();
                        }
                    }
                } catch (IOException e) {
                    log("[!] Input error: " + e.getMessage());
                }
            });
            inputThread.setDaemon(true);

            outputThread.start();
            inputThread.start();

            int exitCode = process.waitFor();
            log("[!] Server exited with code: " + exitCode);
            sendWebhook(webhook, "Server stopped with code `" + exitCode + "`", 0xc0392b);

            return new LaunchResult(exitCode, autoRestart);

        } catch (Exception e) {
            log("[X] Failed to launch server: " + e.getMessage());
            return new LaunchResult(1, autoRestart);
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

            conn.getResponseCode();
        } catch (Exception e) {
            log("[!] Webhook error: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
    }

    private static void printBanner() {
        System.out.println("+------------------------+");
        System.out.println("|     Wrapidly 1.0.6     |");
        System.out.println("+------------------------+");
    }

    private static class LaunchResult {
        int exitCode;
        boolean autoRestart;

        LaunchResult(int code, boolean restart) {
            this.exitCode = code;
            this.autoRestart = restart;
        }
    }
}
