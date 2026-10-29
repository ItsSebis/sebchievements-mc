package dev.sebi.sebchievements.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@CacheableTask
public abstract class GenerateAdvancements extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getCatalog();

    @Input
    public abstract Property<String> getNamespace();

    /** {@code data_major} of the target server's datapack format (Paper 26.3: 121). */
    @Input
    public abstract Property<Integer> getDataMajor();

    @Input
    public abstract Property<Integer> getDataMinor();

    @OutputDirectory
    public abstract DirectoryProperty getResourcesDir();

    @OutputDirectory
    public abstract DirectoryProperty getJavaDir();

    @TaskAction
    public void generate() throws IOException {
        Path resources = getResourcesDir().get().getAsFile().toPath();
        Path java = getJavaDir().get().getAsFile().toPath();
        // wipe first so advancements removed from the catalog don't linger
        clean(resources);
        clean(java);
        CatalogGenerator.generate(getCatalog().get().getAsFile().toPath(), getNamespace().get(),
                getDataMajor().get(), getDataMinor().get(), resources, java);
    }

    private static void clean(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(dir)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
