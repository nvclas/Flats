# Flats Plugin

![Latest Release](https://img.shields.io/github/v/release/nvclas/Flats)
![License](https://img.shields.io/github/license/nvclas/Flats)

Flats is a Minecraft plugin for Paper that allows you to create areas and let your players claim them. It provides an
easy way to manage plots and ensure that players have their own designated areas to build and interact in.

## Features

- **Create Flats:** Easily create areas with the selection tool and commands.
- **Claim Flats:** Let players claim areas, allowing them to build only in their designated flats.
- **Manage Flats:** Add or remove trusted players to allow them building in your flat.
- **Interactive Commands:** Use simple commands to interact with the plugin.

## Installation

1. Download the latest version of the plugin from [Latest release](https://github.com/nvclas/Flats/releases/latest).
2. Copy the downloaded `Flats-x.x.jar` file into the `plugins` folder of your Paper Minecraft server.
3. Restart the server to load the plugin.

## Getting Started

Once the plugin is installed, follow these steps to create and claim your first flat:

1. **Select an area:**
   - Use `/flats select` to get the selection tool.
   - Mark two corners of a cubic area you want to define as a flat.
   - One block at any bottom corner and one block at the opposite top corner.

2. **Create a flat:**
   - Run `/flats add <name>` to register the selected area as a flat.

3. **Claim the flat:**
   - Stand inside the area and use `/flats claim` to make it yours.

4. **Manage access:**
   - Use `/flats trust <player>` to allow other players to build in your flat.
   - Use `/flats untrust <player>` to remove their access.

You're now ready to use Flats and manage your own space in the world!

## Configuration

After installation, the plugin will automatically create the configuration files `flats.yml` and `settings.yml` in the
plugin directory. These files can be customized to control the plugin's behavior. `flats.yml` will save any created
flats and their attributes whereas `settings.yml` contains general settings like language.

## Supported Languages

You can change the active language by editing the language code inside the `settings.yml`.
Currently, the plugin supports the following languages:

- `de_de` - Deutsch (Deutschland)
- `en_us` - English (USA)
- `es_es` - Español (España)
- `fr_fr` - Français (France)

## Commands

The plugin provides several commands to interact with the areas:

- `/flats select` - Get the selection tool for defining the area of a flat.
- `/flats add <name>` - Creates a new flat with the specified name.
- `/flats remove <name>` - Deletes the flat with the given name.
- `/flats list` - Lists all available flats and their owners.
- `/flats update` - Easily update the plugin to the latest version.
- `/flats claim` - Claims the flat the player is currently in.
- `/flats unclaim` - Unclaims the flat the player is currently in.
- `/flats info` - Shows information about the flat the player is currently in.
- `/flats show` - Show every flat my marking it with yellow glass.
- And more

## Permissions

The plugin uses a permission system to control access to certain commands:

- `flats.admin` - Permission for administrative commands like creating and deleting flats.

## Developer

Flats was developed by [nvclas](https://github.com/nvclas).

## License

This project is licensed under the [GNU General Public License v3.0](https://github.com/nvclas/Flats/blob/main/LICENSE).
