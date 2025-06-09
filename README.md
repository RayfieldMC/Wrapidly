---

![Banner](https://github.com/user-attachments/assets/5aadc164-ee91-4ad5-92f9-275d80bacb84)

# 🎮 Minecraft Server Launcher Wrapper

A **lightweight Java launcher wrapper** for Minecraft servers — perfect for hosting panels like **Pterodactyl**!
Customize JVM args, remap commands (like `stop` → `end` for BungeeCord), and get slick Discord webhook alerts on server start/stop.

Made with 💖 by [Rarfield](https://youtube.com/@Rarfield)
Consider joining our [Discord](https://discord.gg/3BGG8tcvVR)

---

## 📜 License

This project is licensed under the MIT License.
Feel free to use, modify, and share — just give credit!

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Features

* **Custom JVM arguments** via `wrapper.properties`
* **Command remapping** using `remap.` keys in `wrapper.properties`
* **Discord webhook support** → server start/stop alerts
* Proxy console input/output → your server runs smoothly
* **Standalone `.jar`** — no external dependencies required!

---

## 🛠 Getting Started

### 1️⃣ Upload Files

* Upload `Launcher.jar` to your server’s working directory.
* On first run, `wrapper.properties` will be auto-generated:

```properties
# JVM command line to launch your server
jvm=java -jar server.jar

# Discord webhook URL to send server start/stop messages (optional)
webhook=

# Command remapping (optional)
# To remap a command, use this format:
# remap.<original_command>=<new_command>
#
# Example remaps (uncomment to use):
# remap.stop=end
# remap.restart=restartwrapper
```

---

### 2️⃣ Configure Pterodactyl

Set your **Startup Command** to:

```bash
java -jar Launcher.jar
```

---

### 3️⃣ Tips

✅ You can edit `wrapper.properties` at any time.
✅ No extra files required → single config file!
✅ Supports modern Java (tested on Java 21).

---
