package de.nvclas.flats.core.commands;

import de.nvclas.flats.core.BaseFlats;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a set of subcommands associated with main command context.
 * Each enumerated value corresponds to a specific subcommand action.
 */
@Getter
public enum MainSubCommand {
    SELECT("select"),
    ADD("add"),
    REMOVE("remove"),
    CLAIM("claim"),
    UNCLAIM("unclaim"),
    TRUST("trust"),
    UNTRUST("untrust"),
    INFO("info"),
    LIST("list"),
    SHOW("show"),
    UPDATE("update");

    private final String subCommandName;

    MainSubCommand(String subCommandName) {
        this.subCommandName = subCommandName;
    }

    /**
     * Constructs the fully qualified name of the command by combining the main command name and the subcommand name.
     *
     * @param plugin  the plugin instance containing the main command name.
     * @param command the subcommand whose name is required.
     * @return a string representing the full command name (e.g., "flats add").
     */
    public static String getFullCommandName(@NotNull BaseFlats plugin, @NotNull MainSubCommand command) {
        return plugin.getMainCommandName() + " " + command.getSubCommandName();
    }
}
