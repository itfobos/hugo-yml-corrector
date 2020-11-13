package hugo.yml.corrector;

import hugo.yml.corrector.markdown.FileContent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Illegal CL arguments. Should be just path to dir.");
            return;
        }

        String pathToProcess = args[0];
        File srcDir = new File(pathToProcess);

        if (!srcDir.exists()) {
            System.err.println("Path " + pathToProcess + " not exist.");
            return;
        }
        if (!srcDir.isDirectory()) {
            System.err.println("Path " + pathToProcess + " is not a directory.");
            return;
        }

        System.out.println("Directory: " + pathToProcess);

        try (Stream<Path> pathsStream = Files.walk(srcDir.toPath())) {
            pathsStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.toString().endsWith("_index.html"))
                    .map(FileContent::of)
                    .forEach(FileContent::adjustYmlPart);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
