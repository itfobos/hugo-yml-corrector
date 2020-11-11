package hugo.yml.corrector.markdown;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HugoMarkdownFile {

    public final Path filePath;

    public HugoMarkdownFile(Path filePath) {
        this.filePath = filePath;
    }

    public void adjustYmlPart() {
        System.out.println("Processing '" + filePath + "' ... ");

        FileContent fileContent = FileContent.of(filePath);

        reorganizeMetadata(fileContent);
        fixImageProperty(fileContent);

        fileContent.save();
    }

    void fixImageProperty(FileContent fileContent) {
        //TODO: move from the method or remane it
        final String auth = (String) fileContent.yaml.get(AUTHOR_YML_PROPERTY);
        if (auth == null) {
            fileContent.yaml.put(AUTHOR_YML_PROPERTY, "");
        }

        Optional.ofNullable(fileContent.yaml.get(IMAGE_YML_PROPERTY))
                .map(o -> (String) o)
                .filter(val -> !val.startsWith("/"))
                .map(val -> '/' + val)
                .ifPresent(val -> fileContent.yaml.put(IMAGE_YML_PROPERTY, val));
    }

    void reorganizeMetadata(FileContent fileContent) {
        if (fileContent.yaml.containsKey(METADATA_YML_PROPERTY)) return;

        final Map<String, Object> metadata = fileContent.yaml.entrySet()
                .stream()
                .filter(e -> METADATA_PROPERTIES.contains(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!metadata.isEmpty()) {
            fileContent.yaml.put(METADATA_YML_PROPERTY, metadata);
            METADATA_PROPERTIES.forEach(fileContent.yaml::remove);
        }
    }

    private static final Set<String> METADATA_PROPERTIES = Set.of("seodescription", "pagetitle", "noindexpage", "canonical");
    private static final String IMAGE_YML_PROPERTY = "image";
    public static final String METADATA_YML_PROPERTY = "metadata";
    public static final String AUTHOR_YML_PROPERTY = "author";
}
