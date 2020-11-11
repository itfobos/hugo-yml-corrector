package hugo.yml.corrector.markdown;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static hugo.yml.corrector.markdown.FileContent.FIELDS_DELIMITER;
import static hugo.yml.corrector.markdown.FileContent.IMAGE_YML_PROPERTY;
import static hugo.yml.corrector.markdown.FileContent.METADATA_PROPERTIES;
import static hugo.yml.corrector.markdown.FileContent.METADATA_YML_PROPERTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileContentTest {
    @Test
    public void nullablePropertiesAreRemovedAfterAdjustments() {
        final String keyForNull = "keyForNull";
        final String keyForNonNull = "keyForNonNull";

        final FileContent content = contentFromYamlKeyValues(keyForNull, null, keyForNonNull, "some value");
        content.removeNullableProperties();

        assertTrue(content.yaml.containsKey(keyForNonNull));
        assertFalse(content.yaml.containsKey(keyForNull));
    }

    @Test
    public void metadataPropertiesAreOrganizedInObject() {
        final String valuePrefix = "value for ";
        final Map<String, Object> ymlMap = METADATA_PROPERTIES
                .stream()
                .collect(Collectors.toMap(Function.identity(), propName -> valuePrefix + propName));

        final FileContent content = new FileContent(null, ymlMap, null);

        content.reorganizeMetadata();

        METADATA_PROPERTIES.forEach(popName -> assertFalse(content.yaml.containsKey(popName)));

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) content.yaml.get(FileContent.METADATA_YML_PROPERTY);
        assertThat(metadata, CoreMatchers.notNullValue());

        METADATA_PROPERTIES.forEach(propName -> {
            assertTrue(metadata.containsKey(propName));
            assertTrue(metadata.containsValue(valuePrefix + propName));
        });
    }

    @Test
    public void existedMetadataObjectWontBeReorganized() {
        final FileContent content = contentFromYamlKeyValues(
                METADATA_YML_PROPERTY, "some metadata",
                "another property", "another value"
        );

        var beforeYmlMap = Map.copyOf(content.yaml);
        content.reorganizeMetadata();

        assertThat(content.yaml, equalTo(beforeYmlMap));
    }

    @Test
    public void emptyMetadataObjectWillNotBePlacedInYaml() {
        final FileContent content = contentFromYamlKeyValues(
                "not metadata property", "some value",
                "another property", "another value"
        );

        content.reorganizeMetadata();

        assertFalse(content.yaml.containsKey(METADATA_YML_PROPERTY));
    }

    @Test
    public void imagePathIsChangedCorrectly() {
        final FileContent content = contentFromYamlKeyValues(
                IMAGE_YML_PROPERTY, "path/to/image.jpg",
                "another property", "another value"
        );

        assertThat((String) content.yaml.get(IMAGE_YML_PROPERTY), not(startsWith("/")));

        content.fixImageProperty();

        assertThat((String) content.yaml.get(IMAGE_YML_PROPERTY), startsWith("/"));
    }

    @Test
    public void fullCircleWorksWithoutExceptions() throws IOException {
        final Path tempFile = prepareTestFile();

        final FileContent content = FileContent.of(tempFile);
        content.adjustYmlPart();

        Files.delete(tempFile);
    }

    private Path prepareTestFile() throws IOException {
        final Path tempFile = Files.createTempFile("test", "");

        StringBuilder testContent = new StringBuilder(FIELDS_DELIMITER).append('\n');

        METADATA_PROPERTIES.forEach(prop -> {
            testContent.append(prop).append(": ").append("value for ").append(prop).append('\n');
        });

        testContent.append(IMAGE_YML_PROPERTY).append(": ").append("path/to/image").append('\n');
        testContent.append("another property").append(": ").append("another value").append('\n');

        testContent.append(FIELDS_DELIMITER).append('\n');

        testContent.append("some doc body text");

        Files.writeString(tempFile, testContent.toString());
        return tempFile;
    }

    FileContent contentFromYamlKeyValues(String k1, Object v1, String k2, Object v2) {
        final HashMap<String, Object> ymlMap = new HashMap<>();
        ymlMap.put(k1, v1);
        ymlMap.put(k2, v2);

        return new FileContent(null, ymlMap, null);
    }
}
