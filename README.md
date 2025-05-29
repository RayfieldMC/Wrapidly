# JVM Wrapper Launcher
A lightweight and flexible Java-based wrapper designed to launch target JAR files with custom JVM arguments. Ideal for environments or hosting platforms where direct modification of JVM parameters is restricted. Simply configure your desired arguments and target application, and this wrapper handles the rest.

---

## How to Use
- Download ```Launcher.jar```
- Place it on your root directory where ```server.jar``` is located
- Make the startup executable ```Launcher.jar``` instead of ```server.jar```

---

## How to Edit the Startup Command
- Open ```launcher.properties```
- You can change all the JVM arguments there

---

## 🛠️ How to Build the Launcher `.jar`

### 📁 Project Structure
```
project-root/
├── src/
│   └── Launcher.java
├── manifest.txt
└── launcher.properties (if you are testing)
```

### 📦 Steps to Build

#### 1. **Compile the Code**
Run this in your terminal to compile the `Launcher.java` file into the `build/` folder:
```bash
javac -d build src/Launcher.java
```

#### 2. **Create a Manifest File**
Make a file called `manifest.txt` with this inside:
```
Manifest-Version: 1.0
Main-Class: Launcher
```
> ⚠️ Make sure there's a blank line at the end of the file!

#### 3. **Package It as a `.jar`**
Now run this to build your launcher JAR:
```bash
jar cfm launcher.jar manifest.txt -C build/ .
```

- `c` – create
- `f` – output to file
- `m` – include manifest
- `-C build/ .` – include all compiled class files

#### 4. **Run the Launcher**
```bash
java -jar launcher.jar
```

---

Made with 💖 by [VyxialX](https://youtube.com/@VyxialX)
