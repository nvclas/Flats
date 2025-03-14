package de.nvclas.flats.commands.flats;

import lombok.Getter;

/**
 * Represents a set of subcommands associated with the "flats" command context.
 * Each enumerated value corresponds to a specific subcommand action.
 */
@Getter
public enum FlatsSubCommand {
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

    FlatsSubCommand(String commandName) {
        this.subCommandName = commandName;
    }

    /**
     * Constructs and returns the full command name by appending the subcommand name
     * to the base "flats" command.
     *
     * @return The full command name in the format "flats {subCommandName}", where
     * {@code subCommandName} is the specific subcommand assigned to this instance.
     */
    public String getFullCommandName() {
        return "flats " + subCommandName;
    }
}
