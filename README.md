# 🎮 Minecraft Server Launcher Wrapper

A **lightweight Java launcher wrapper** for Minecraft servers — perfect for hosting panels like **Pterodactyl**!  
Customize JVM args, remap commands (like `stop` → `end` for BungeeCord), and get slick Discord webhook alerts on server start/stop.

---

## 🚀 Features

- **Custom JVM arguments** via `launcher.properties`  
- **Command remapping** with `remap.properties` (optional & secret)  
- **Discord webhook support** (`launcher-webhook.properties`) with clean embed messages  
- Proxy console input/output, so your server runs smoothly  
- **Standalone `.jar`** — no external dependencies!

---

## 🛠 Getting Started

### 1. Upload Files

- Upload `Launcher.jar` to your server’s working directory.  
- Create or upload these config files:  

  - `launcher.properties`  
    ```properties
    java -javaagent:authlibinjector.jar=ely.by -jar server.jar
    ```

  - `launcher-webhook.properties` (optional)  
    ```properties
    webhookUrl=https://discord.com/api/webhooks/your_webhook_url_here
    ```

  - `remap.properties` (optional and you need to create the remap.properties file on your own)  
    ```properties
    stop=end
    restart=stop
    ```

---

### 2. Configure Pterodactyl

Set your startup command to:  
```bash
java -jar Launcher.jar
```

If you have only access to server jar then set your server jar file to
```bash
Launcher.jar
```
