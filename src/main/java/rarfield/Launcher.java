package rarfield;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class Launcher {

    public static void main(String[] args) {
        addShutdownHook();

        while (true) {
            LaunchResult result = launchOnce();

            if (result.exitCode == 0) {
                logInfo("[OK] Server stopped normally. Exiting Wrapidly.");
                break;
            }

            if (!result.autoRestart) {
                logError("[X] Server crashed and autoRestart is disabled. Exiting Wrapidly.");
                break;
            }

            logWarn("[!] Server crashed. Restarting due to autoRestart = true.");
        }
    }

    private static LaunchResult launchOnce() {
        printBanner();

        File jar = new File("server.jar");
        if (!jar.exists()) {
            logError("[X] server.jar not found.");
            return new LaunchResult(1, false);
        } else {
            logInfo("[OK] server.jar found.");
        }

        Config config = Config.loadOrCreate("wrapper.yml");
        if (!config.valid) {
            logError("[X] Failed to load wrapper.yml: " + config.errorMessage);
            return new LaunchResult(1, false);
        }

        if (!config.preStartCommands.isEmpty()) {
            logInfo("[●] Running pre-start commands...");
            for (String cmd : config.preStartCommands) {
                try {
                    logPlain("    > " + cmd);
                    Process p = new ProcessBuilder(parseCommand(cmd)).inheritIO().start();
                    if (!p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
                        logWarn("[!] Command timed out: " + cmd);
                        p.destroy();
                    }
                } catch (Exception e) {
                    logError("[!] Failed: " + e.getMessage());
                }
            }
        }

        logInfo("[●] Launching server with: " + config.jvmCommand);
        sendWebhook(config.webhook, "Server starting...", 0x3aa856);

        try {
            ProcessBuilder pb = new ProcessBuilder(parseCommand(config.jvmCommand));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logPlain(line);
                        if (config.requireHealthyStartup && line.toLowerCase().contains("exception")) {
                            logError("[X] Detected error in startup!");
                            sendWebhook(config.webhook, "Startup error detected.", 0xff0000);
                        }
                    }
                } catch (IOException e) {
                    logError("[!] Output error: " + e.getMessage());
                }
            });
            outputThread.setDaemon(true);

            Thread inputThread = new Thread(() -> {
                try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                    String input;
                    while ((input = console.readLine()) != null) {
                        input = input.trim();
                        if (input.isEmpty()) continue;

                        if (config.remap.containsKey(input)) {
                            String newCmd = config.remap.get(input);
                            logWarn("[!] Remap: " + input + " → " + newCmd);
                            input = newCmd;
                        }

                        if (config.macros.containsKey(input)) {
                            logWarn("[!] Macro: " + input);
                            for (String macroCmd : config.macros.get(input)) {
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
                    logError("[!] Input error: " + e.getMessage());
                }
            });
            inputThread.setDaemon(true);

            outputThread.start();
            inputThread.start();

            int exitCode = process.waitFor();
            logWarn("[!] Server exited with code: " + exitCode);
            sendWebhook(config.webhook, "Server stopped with code `" + exitCode + "`", 0xc0392b);

            return new LaunchResult(exitCode, config.autoRestart);

        } catch (Exception e) {
            logError("[X] Failed to launch server: " + e.getMessage());
            e.printStackTrace();
            return new LaunchResult(1, config.autoRestart);
        }
    }

    private static void sendWebhook(String url, String msg, int color) {
        if (url == null || url.isEmpty()) return;
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = String.format("""
                        {
                          "embeds": [{
                            "title": "%s",
                            "color": %d,
                            "timestamp": "%s"
                          }]
                        }
                        """, msg, color, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 300) {
                    return;
                } else {
                    logError("[!] Webhook HTTP error: " + responseCode);
                }
            } catch (Exception e) {
                logError("[!] Webhook error (attempt " + attempt + "): " + e.getMessage());
                try { Thread.sleep(1000 * attempt); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ANSI color codes for nicer output
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static void logInfo(String msg) {
        System.out.println(GREEN + timestamp() + " " + msg + RESET);
    }
    private static void logWarn(String msg) {
        System.out.println(YELLOW + timestamp() + " " + msg + RESET);
    }
    private static void logError(String msg) {
        System.out.println(RED + timestamp() + " " + msg + RESET);
    }
    private static void logPlain(String msg) {
        System.out.println(msg);
    }
    private static String timestamp() {
        return "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]";
    }

    private static void printBanner() {
        System.out.println(CYAN +
            "+--------------------------+\n" +
            "|    Wrapidly 1.0.6 IMP    |\n" +
            "+--------------------------+" + RESET);
        System.out.println(GREEN + "# " + timestamp() + " JVM: " + System.getProperty("java.version") + RESET);
    }

    /** Graceful shutdown hook */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logWarn("[!] Wrapidly is shutting down gracefully.");
        }));
    }

    /** Helper for parsing shell commands, supporting quoted strings. */
    private static List<String> parseCommand(String cmd) {
        // This splits by space but respects quoted substrings
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder token = new StringBuilder();
        char quoteChar = 0;
        for (int i = 0; i < cmd.length(); i++) {
            char c = cmd.charAt(i);
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
                continue;
            }
            if (inQuotes && c == quoteChar) {
                inQuotes = false;
                continue;
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) tokens.add(token.toString());
        return tokens;
    }

    /**
     * Configuration loading, validation, and parsing.
     */
    private static class Config {
        public String jvmCommand = "java -jar server.jar";
        public String webhook = "";
        public boolean autoRestart = false;
        public boolean requireHealthyStartup = false;
        public Map<String, String> remap = new HashMap<>();
        public Map<String, List<String>> macros = new HashMap<>();
        public List<String> preStartCommands = new ArrayList<>();
        public boolean valid = true;
        public String errorMessage = "";

        static final String defaultConfig = """
                # Wrapidly Config Example
                jvm: java -jar server.jar        # JVM launch command
                webhook: ""                      # Discord webhook URL or similar

                remap:
                  # stop: end                    # Remap 'stop' command to 'end'

                macros:
                  # reboot: |
                  #   say Rebooting...
                  #   save-all
                  #   stop

                requireHealthyStartup: true      # Detect startup errors
                autoRestart: true                # Restart on crash

                preStartCommands:
                  # - echo Preparing to launch...
                  # - mkdir logs
                """;

        public static Config loadOrCreate(String path) {
            Config conf = new Config();
            File configFile = new File(path);

            try {
                if (!configFile.exists()) {
                    Files.writeString(configFile.toPath(), defaultConfig);
                    conf.valid = true;
                    logInfo("[OK] Created default wrapper.yml.");
                }

                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> config = yaml.load(new FileInputStream(configFile));

                conf.jvmCommand = String.valueOf(config.getOrDefault("jvm", conf.jvmCommand)).trim();
                conf.webhook = String.valueOf(config.getOrDefault("webhook", conf.webhook)).trim();
                conf.autoRestart = Boolean.parseBoolean(String.valueOf(config.getOrDefault("autoRestart", conf.autoRestart)));
                conf.requireHealthyStartup = Boolean.parseBoolean(String.valueOf(config.getOrDefault("requireHealthyStartup", conf.requireHealthyStartup)));

                if (config.get("remap") instanceof Map<?, ?> map) {
                    map.forEach((k, v) -> conf.remap.put(String.valueOf(k), String.valueOf(v)));
                }

                if (config.get("macros") instanceof Map<?, ?> map) {
                    for (Object key : map.keySet()) {
                        String[] lines = String.valueOf(map.get(key)).split("\\r?\\n");
                        List<String> macroLines = new ArrayList<>();
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) macroLines.add(line.trim());
                        }
                        conf.macros.put(String.valueOf(key), macroLines);
                    }
                }

                if (config.get("preStartCommands") instanceof List<?> list) {
                    for (Object o : list) conf.preStartCommands.add(String.valueOf(o));
                }

                // Validation
                if (conf.jvmCommand.isEmpty()) {
                    conf.valid = false;
                    conf.errorMessage = "JVM command is blank in config!";
                }

            } catch (Exception e) {
                conf.valid = false;
                conf.errorMessage = e.getMessage();
            }

            return conf;
        }
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