package com.nozz.ambuscade;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server settings in config/ambuscade.properties, written with the defaults the first time. Read once at start-up. */
public final class AmbuscadeConfig {
    private static final Logger LOG = LoggerFactory.getLogger("ambuscade");
    public static final int MORTAR_RANGE_LIMIT = 96;   // what the shell can reach and still land where it was told

    /** Farthest a mortar fires, in blocks. Servers can lower it. */
    public static int mortarMaxRange = MORTAR_RANGE_LIMIT;

    private AmbuscadeConfig() {
    }

    static void load(Path dir) {
        Path file = dir.resolve("ambuscade.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file)) {
                p.load(in);
            } catch (IOException e) {
                LOG.warn("Could not read {}, using defaults", file, e);
            }
        }
        mortarMaxRange = clamp(p.getProperty("mortar.max_range"), 8, MORTAR_RANGE_LIMIT, MORTAR_RANGE_LIMIT);
        if (Files.exists(file)) return;
        try {
            Files.createDirectories(dir);
            try (Writer out = Files.newBufferedWriter(file)) {
                out.write("# Ambuscade server settings\n");
                out.write("# Farthest a mortar can fire, in blocks (8 to " + MORTAR_RANGE_LIMIT + "). Shells keep the chunks they fly over loaded.\n");
                out.write("mortar.max_range=" + mortarMaxRange + "\n");
            }
        } catch (IOException e) {
            LOG.warn("Could not write {}", file, e);
        }
    }

    private static int clamp(String value, int min, int max, int fallback) {
        try {
            return value == null ? fallback : Math.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException e) {
            LOG.warn("Ignoring bad number '{}' in ambuscade.properties", value);
            return fallback;
        }
    }
}
