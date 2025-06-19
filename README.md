![Banner](https://github.com/user-attachments/assets/2f9c92d5-071f-4181-9975-36a13f1a6dbd)
# Wrapidly - JVM Server Launcher Wrapper

Wrapidly is a lightweight, standalone Java launcher wrapper for Minecraft servers — built for hosting panels like Pterodactyl, but works anywhere.

Clean logs  
Console command remapping  
Discord webhook alerts  
Configurable with `wrapper.yml`

Made by [Rarfield](https://youtube.com/@Rarfield)  
Join the community on [Discord](https://discord.gg/3BGG8tcvVR)

---

## Features

- Custom JVM launch command via `wrapper.yml`
- Console command remapping (e.g. `stop → end`)
- Discord webhook alerts on server start and stop
- Clean input/output passthrough
- Launcher exits automatically when the server process ends

---

## Setup

### 1. Upload Files

- Upload `Wrapidly.jar` to your server folder.
- If you're building it yourself, include `snakeyaml-2.4.jar` in a `libs/` directory.

---

### 2. First Run Generates Config

Wrapidly creates `wrapper.yml` automatically if it doesn’t exist:

```yaml
# Wrapidly Config
jvm: java -jar server.jar

webhook: ""  # Optional: Discord webhook URL

remap:
  # stop: end
  # restart: stop
```

---

## Usage

Use this as your server startup command:

```bash
java -Xms128M -XX:MaxRAMPercentage=95.0 -Dterminal.jline=false -Dterminal.ansi=true -jar Wrapidly.jar
```

Wrapidly will:
- Start your Minecraft server using the command in `wrapper.yml`
- Pipe console output and input directly
- Support remapped commands
- Send optional webhook alerts
- Exit with the same code as the server

---

## Command Remapping

The `remap` section allows you to rewrite console commands in real time. This is useful for tools like BungeeCord, which require custom shutdown commands.

Example:

```yaml
remap:
  stop: end
  restart: stop
```

Typing `stop` in the console will actually send `end` to the server process.

---

## License

Licensed under the MIT License.  
You're free to use, modify, and redistribute — just credit the author.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
