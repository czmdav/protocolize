
<div align=center>
    <img src="./protocolize-logo.webp" width="512">
    <br/><br/>
    <p>Next generation protocol manipulation framework for proxy servers.</p>
    <a href="https://github.com/czmdav/protocolize/releases"><img src="https://img.shields.io/github/downloads/czmdav/protocolize/total?color=GREEN&label=Downloads"></a>
    <a href="https://github.com/czmdav/protocolize/issues"><img src="https://img.shields.io/github/issues/czmdav/protocolize?label=Issues"></a>
    <a href="https://github.com/czmdav/protocolize/pulls"><img src="https://img.shields.io/github/issues-pr/czmdav/protocolize?label=Pull%20requests"></a>
    <a href="https://github.com/czmdav/protocolize/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/czmdav/protocolize"></a>
</div>

> **Note:** This repository is a fork of [Exceptionflug's Protocolize](https://github.com/Exceptionflug/Protocolize).

# Protocolize3
This is the official repository for the next generation proxy server protocol manipulation framework. If you wish to see the repository for Protocolize v1, click [here](https://github.com/Exceptionflug/protocolize/tree/v1).

## Getting started
### Installation
In order to use Protocolize, you have to install it onto your proxy server. Protocolize is supporting BungeeCord and Velocity proxy servers.
Download the latest protocolize version for your proxy software right here: https://exceptionflug.de/protocolize.
### Maven dependency and repository
The API is published through [JitPack](https://jitpack.io/#czmdav/protocolize).
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.czmdav.protocolize</groupId>
    <artifactId>protocolize-api</artifactId>
    <version>v3.0.0</version>
    <scope>provided</scope>
</dependency>
```
**Or alternatively, with Gradle:**
```kotlin
repositories {
    maven {
        url = uri('https://jitpack.io')
    }
}

dependencies {
    compileOnly("com.github.czmdav.protocolize:protocolize-api:v3.0.0")
}
```

## Compatibility

Protocolize is shipped with its default data module which adds support for the following versions:
| Minecraft Version | Supported |
|--|--|
| ≤ 1.12.2 | ❌ |
| 1.13 - 1.20.6 | ✔️ |
| 1.21 - 1.21.11 | ✔️ |
| 26.1 - 26.3 | ✔️ (Requires a Velocity build supporting the version) |

Protocolize requires Velocity 4.0.0 or newer running on Java 25.

Implemented packets by default:

| Packet | Class |
|--|--|
| Player block placement | dev.simplix.protocolize.data.packets.BlockPlacement |
| Inventory click | dev.simplix.protocolize.data.packets.ClickWindow |
| Close inventory | dev.simplix.protocolize.data.packets.CloseWindow |
| Confirm transaction | dev.simplix.protocolize.data.packets.ConfirmTransaction |
| Held item change | dev.simplix.protocolize.data.packets.HeldItemChange |
| Named sound effect | dev.simplix.protocolize.data.packets.NamedSoundEffect |
| Sound effect | dev.simplix.protocolize.data.packets.SoundEffect |
| Open inventory | dev.simplix.protocolize.data.packets.OpenWindow |
| Player look update | dev.simplix.protocolize.data.packets.PlayerLook |
| Player position update | dev.simplix.protocolize.data.packets.PlayerPosition |
| Player position and look update | dev.simplix.protocolize.data.packets.PlayerPositionLook |
| Player slot update | dev.simplix.protocolize.data.packets.SetSlot |
| Player item interaction | dev.simplix.protocolize.data.packets.UseItem |
| Inventory items | dev.simplix.protocolize.data.packets.WindowItems |
| Inventory properties | dev.simplix.protocolize.data.packets.WindowProperty |

You can easily add support for new packets and versions by installing modules.


## Known issues
- Protocolize is unstable when being used with **proxy installations** of Geyser / Floodgate and/or ViaVersion.

## Get some help
For documentation take a look at the wiki pages.
If you have questions feel free to join our [discord server](https://discord.gg/ZsYjwcNRbV).
