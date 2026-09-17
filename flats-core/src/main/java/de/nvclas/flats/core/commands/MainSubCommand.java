package de.nvclas.flats.core.commands;

import lombok.Getter;

/**
 * Represents a set of subcommands associated with the "flats" command context.
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
