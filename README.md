![Banner](https://github.com/user-attachments/assets/2f9c92d5-071f-4181-9975-36a13f1a6dbd)
# Wrapidly - JVM Server Launcher Wrapper

**Wrapidly** is a lightweight, standalone Java launcher wrapper for Minecraft servers. Made for changing the **JVM Arguments** even if your server hosting doesn't allow it.

- Clean logs  
- Console command remapping  
- Discord webhook alerts  
- Customizable via `wrapper.yml`

Made with 💖 by [Rarfield](https://youtube.com/@Rarfield)  
Come vibe in our [Discord](https://discord.gg/3BGG8tcvVR)

---

## ✨ Features

- **Custom JVM Command** via `wrapper.yml`
- **Command Remapping** (e.g. `stop → end`)
- **Discord Webhook Alerts** when the server starts or stops
- Clean console I/O passthrough

---

## ⚙️ Setup

### 1. Add Files to Your Server

- Upload `Wrapidly.jar` to your server folder  
- If needed, include `snakeyaml-2.4.jar` in `libs/` if you're building from source

---

### 2. First Run = Auto Setup

On the first launch, `wrapper.yml` will be auto-created:

```yaml
# Wrapidly Config
jvm: java -jar server.jar

webhook: ""  # Optional: Discord webhook URL

remap:
  # stop: end
  # restart: stop
````

---

## ▶️ How to Launch

**Startup command:**

```bash
java -Xms128M -XX:MaxRAMPercentage=95.0 -Dterminal.jline=false -Dterminal.ansi=true -jar Wrapidly.jar
```

This will:

* Start your Minecraft server with the `jvm:` command
* Pipe input/output through the console
* Send webhook notifications if configured
* Shut down cleanly when the server ends

---

## 🔁 Command Remapping?

Yup. This lets you map any input command to a different one — perfect for stuff like BungeeCord that needs special exit commands.

Example:

```yaml
remap:
  stop: end
  restart: stop
```

So when you type `stop`, Wrapidly will actually send `end`.

---

## License

Licensed under the **MIT License** —
Use it, fork it, ship it, just give credit 🙏

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
