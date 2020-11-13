package hugo.yml.corrector.markdown;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileContent {

    static final String METADATA_YML_PROPERTY = "metadata";
    static final String IMAGE_YML_PROPERTY = "image";
    static final Set<String> METADATA_PROPERTIES = Set.of(
            "seodescription", "pagetitle", "noindexpage", "canonical"
    );
    static final String FIELDS_DELIMITER = "---";

    public final Path filePath;
    public final Map<String, Object> yaml;
    public final List<String> srcLines;

    private static final Yaml READ_YAML = new Yaml();
    private static final Yaml WRITE_YAML;


    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        WRITE_YAML = new Yaml(options);
    }

    FileContent(Path filePath, Map<String, Object> yaml, List<String> srcLines) {
        this.filePath = filePath;
        this.yaml = yaml;
        this.srcLines = srcLines;
    }

    public void adjustYmlPart() {
        System.out.print("Processing '" + filePath.getFileName() + "' ... ");

        removeNullableProperties();
        reorganizeMetadata();
        fixImageProperty();

        save();

        System.out.println("Finished");
    }

    void save() {
        StringWriter writer = new StringWriter();
        WRITE_YAML.dump(yaml, writer);

        String pageBody = srcLines.stream()
                .filter(new IsYmlLinePredicate().negate().and(line -> !line.startsWith(FIELDS_DELIMITER)))
                .collect(Collectors.joining("\n"));

        StringBuilder resultContent = new StringBuilder();
        resultContent
                .append(FIELDS_DELIMITER).append('\n')
                .append(writer.toString())
                .append(FIELDS_DELIMITER).append('\n')
                .append(pageBody).append('\n');

        try {
            Files.writeString(filePath, resultContent);
        } catch (IOException e) {
            throw new RuntimeException("Error storing file " + filePath.toString(), e);
        }
    }

    void reorganizeMetadata() {
        if (yaml.containsKey(METADATA_YML_PROPERTY)) return;

        final Map<String, Object> metadata = yaml.entrySet()
                .stream()
                .filter(e -> METADATA_PROPERTIES.contains(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!metadata.isEmpty()) {
            yaml.put(METADATA_YML_PROPERTY, metadata);
            METADATA_PROPERTIES.forEach(yaml::remove);
        }
    }

    void fixImageProperty() {
        Optional.ofNullable(yaml.get(IMAGE_YML_PROPERTY))
                .map(o -> (String) o)
                .filter(val -> !val.startsWith("/"))
                .map(val -> '/' + val)
                .ifPresent(val -> yaml.put(IMAGE_YML_PROPERTY, val));
    }

    private void removeNullableProperties() {
        final List<Map.Entry<String, Object>> nullableEntries = yaml.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .collect(Collectors.toList());

        nullableEntries.forEach(entry -> {
            System.out.println("Nullable property: " + entry.getKey());
            yaml.remove(entry.getKey());
        });
    }

    public static FileContent of(Path filePath) {
        try {
            List<String> srcLines = List.copyOf(Files.readAllLines(filePath, StandardCharsets.UTF_8));

            String yamlStr = srcLines.stream().filter(new IsYmlLinePredicate()).collect(Collectors.joining("\n"));

            Map<String, Object> yaml = READ_YAML.load(new ByteArrayInputStream(yamlStr.getBytes(StandardCharsets.UTF_8)));

            return new FileContent(filePath, yaml, srcLines);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + filePath.toString(), e);
        }
    }


    static class IsYmlLinePredicate implements Predicate<String> {
        boolean lineIsBetweenDelimiters;

        @Override
        public boolean test(String line) {
            if (line.strip().startsWith(FIELDS_DELIMITER)) {
                lineIsBetweenDelimiters = !lineIsBetweenDelimiters;
                return false;
            }

            return lineIsBetweenDelimiters;
        }
    }

}
