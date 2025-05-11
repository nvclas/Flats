package de.nvclas.flats.updater;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@UtilityClass
public class OldFileRemover {

    /**
     * Deletes files in the current directory whose names end with {@code "_DELETE"}.
     * <p>
     * This method checks all files in the current directory and removes those marked
     * for deletion based on their filename suffix.
     */
    public void deleteMarkedFiles() {
        File[] files = new File(".").listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (!file.getName().endsWith("_DELETE")) {
                continue;
            }
            try {
                Files.delete(file.toPath());
            } catch (IOException ignored) {
                return;
            }
        }
    }
}
