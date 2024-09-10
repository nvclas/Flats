# Flats Plugin

Flats is a Minecraft plugin for Paper that allows you to create areas and assign them to players. It provides an easy way to manage plots and ensure that players have their own designated areas to build and interact in.

## Features

- **Create Areas:** Easily create new areas that can be assigned to players.
- **Assign Areas:** Assign specific areas to players, allowing them to build only in their designated plots.
- **Manage Areas:** Delete or reassign areas as needed.
- **Interactive Commands:** Use simple commands to interact with the plugin.

## Installation

1. Download the latest version of the plugin from [Latest release](https://github.com/nvclas/Flats/releases/latest).
2. Copy the downloaded `Flats-x.x.jar` file into the `plugins` folder of your Paper Minecraft server.
3. Restart the server to load the plugin.

## Configuration

After installation, the plugin will automatically create the configuration files `flats.yml` and `settings.yml` in the plugin directory. These files can be customized to control the plugin's behavior. `flats.yml` will save any created flats and their attributes whereas `settings.yml` contains general settings like language.

## Supported Languages

You can change the active language by editing the language code inside the `settings.yml`.
Currently the plugin supports the following languages:

- `de_de` - Deutsch (Deutschland)
- `en_us` - English (USA)
- `es_es` - Español (España)

## Commands

The plugin provides several commands to interact with the areas:

- `/flats select` - Get the selection tool for defining the area of a flat.
- `/flats create <name>` - Creates a new area with the specified name.
- `/flats claim` - Claims the area the player is currently in.
- `/flats unclaim` - Unclaims the area the player is currently in.
- `/flats info` - Shows information about the area the player is currently in.
- `/flats list` - Lists all available areas and their owners.
- `/flats update` - Easily update the plugin to the latest version.
- Additional features...

## Permissions

The plugin uses a permission system to control access to certain commands:

- `flats.admin` - Permission for administrative commands like listing all areas.

## Developer

Flats was developed by [nvclas](https://github.com/nvclas).

## License

This project is licensed under the Creative Commons Attribution-NonCommercial 4.0 International Public License.