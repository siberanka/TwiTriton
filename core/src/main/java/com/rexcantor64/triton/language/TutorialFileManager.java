package com.rexcantor64.triton.language;

import com.rexcantor64.triton.Triton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class TutorialFileManager {

    private static final String TURKISH_FILE = "tutorial_tr.yml";
    private static final String ENGLISH_FILE = "tutorial_en.yml";

    private TutorialFileManager() {
    }

    public static void ensureTutorials(Triton<?, ?> triton) {
        ensureFolderTutorials(triton, new File(triton.getDataFolder(), "translations"), "translations");

        String platformFolderName = PlatformVariantManager.sanitizeFolderName(
                triton.getConfig().getPlatformVariantsFolder());
        ensureFolderTutorials(triton, new File(triton.getDataFolder(), platformFolderName), "platforms");
    }

    public static boolean isTutorialFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.equals(TURKISH_FILE) || name.equals(ENGLISH_FILE);
    }

    private static void ensureFolderTutorials(Triton<?, ?> triton, File folder, String resourceFolder) {
        Path folderPath = folder.toPath();
        try {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            triton.getLogger().logError(e, "Failed to create tutorial folder %1.", folder.getName());
            return;
        }

        copyIfMissing(triton, folderPath, resourceFolder, TURKISH_FILE);
        copyIfMissing(triton, folderPath, resourceFolder, ENGLISH_FILE);
    }

    private static void copyIfMissing(Triton<?, ?> triton, Path folder, String resourceFolder, String fileName) {
        Path target = folder.resolve(fileName);
        if (Files.exists(target)) {
            return;
        }

        String resourceName = "tutorials/" + resourceFolder + "/" + fileName;
        Path temporary = null;
        try (InputStream input = triton.getLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                triton.getLogger().logError("Tutorial resource %1 is missing from the plugin.", resourceName);
                return;
            }
            temporary = Files.createTempFile(folder, fileName, ".tmp");
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target);
            temporary = null;
            triton.getLogger().logDebug("Created %1/%2 tutorial file.", folder.getFileName(), fileName);
        } catch (FileAlreadyExistsException ignored) {
            // Another reload completed the same create-only operation.
        } catch (IOException e) {
            triton.getLogger().logError(e, "Failed to create tutorial file %1/%2.", folder.getFileName(), fileName);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The incomplete temporary file is never considered a tutorial data file.
                }
            }
        }
    }

}
