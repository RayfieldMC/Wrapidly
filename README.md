![Banner](https://github.com/user-attachments/assets/5aadc164-ee91-4ad5-92f9-275d80bacb84)

# 🚀 Wrapidly - JVM Server Launcher Wrapper

Wrapidly is a **lightweight, standalone Java launcher wrapper** for Minecraft servers — designed for hosting panels like **Pterodactyl**!  
Easily tweak JVM arguments, **remap commands** (like `stop → end` for BungeeCord), and get clean **Discord webhook alerts** when your server starts or stops.

Made with 💖 by [Rarfield](https://youtube.com/@Rarfield)  
Come vibe in our [Discord](https://discord.gg/3BGG8tcvVR)

---

## 📜 License

Licensed under the **MIT License**.  
You’re free to use, modify, and share — just drop a credit 🙌

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 💡 Features

- 🧠 **Custom JVM arguments** from `wrapper.properties`
- 🔄 **Command remapping** using `remap.` keys (ex: remap.stop=end)
- 📣 **Discord webhook** support for start/stop alerts

---

## ⚙️ Getting Started

### 1️⃣ Upload the Files

Just drop `Launcher.jar` into your server folder.  
On the **first run**, a `wrapper.properties` file will auto-generate with all settings:

```properties
# JVM command to start your Minecraft server
jvm=java -jar server.jar

# Discord webhook URL (optional)
webhook=

# Command remapping (optional)
# Format: remap.<original_command>=<replacement_command>
# 
# Examples (uncomment to enable):
# remap.stop=end
# remap.restart=restartwrapper
