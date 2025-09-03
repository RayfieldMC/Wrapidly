> **⚠ Modern Project ⚠**  
>Wrapidly is the upgrade over older forks like LaunchHelper.  
>It’s feature-packed, modern, and perfect for containerized server setups.

# Introduction

Wrapidly is a robust Minecraft server launcher that handles **Java agent injection, auto-restarts, macros, remaps, and pre-start commands**.  
Built for **Java 17+ / Java 21**, it runs smoothly in **Pterodactyl, Multicraft, or any containerized environment**.

---

## ✨ Features
- ✅ **Java 21+ compatible**  
- ✅ **Auto-restart** on server crash  
- ✅ **Pre-start commands**: Run scripts or setup tasks before the server starts  
- ✅ **Command remaps**: Replace commands (e.g., `stop → end`) automatically  
- ✅ **Macros support**: Multi-line commands triggered by a single input  
- ✅ **Startup health detection**: Checks server logs for exceptions on startup  
- ✅ **Webhook notifications**: Sends server status to Discord or other endpoints  
- ✅ **Graceful shutdown hook**: Logs when Wrapidly is shutting down  
- ✅ **Cross-platform**: Works on Windows/Linux without attach.dll issues
- ✅ **Startup jar bypass**: Works on panels that validate server jars, letting you run your custom Minecraft server without panel restrictions

---

## ⚡ How to Use

1. Download `Wrapidly.jar` from [Releases](../../releases).  

2. Place it in your **server root folder**.  

3. Configure `wrapper.yml` (auto-created if missing):

   ```yaml
   jvm: java -jar server.jar
   webhook: ""            # Discord webhook URL or similar
   autoRestart: true
   requireHealthyStartup: true

   remap:
     # stop: end

   macros:
     # reboot:
     #   say Rebooting...
     #   save-all
     #   stop

   preStartCommands:
     # - echo Preparing to launch...
   ```

4. Set your panel to run `Wrapidly-{version}.jar` instead of `server.jar`.

---

## 🔧 Building from Source

Requirements:

* Java 21+
* Gradle

Build:

```bash
gradle clean build
```

Compiled jar will be in:

```
build/libs/Wrapidly-{version}.jar
```

---

>**⚡ Pro tip**  
>Wrapidly actively monitors your server for startup errors, supports macros/remaps, and auto-restarts, making server management way smoother than older forks.
