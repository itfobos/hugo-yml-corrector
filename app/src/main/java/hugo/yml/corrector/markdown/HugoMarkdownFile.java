package hugo.yml.corrector.markdown;

import java.nio.file.Path;

public class HugoMarkdownFile {
    public final Path filePath;

    public HugoMarkdownFile(Path filePath) {
        this.filePath = filePath;
    }

    public void adjustYmlPart() {
        System.out.println("Processing '" + filePath + "' ... ");
        FileContent fileContent = FileContent.of(filePath);
        //TODO: [#3] Adjust SEO metadata
        fileContent.save();
    }
}
