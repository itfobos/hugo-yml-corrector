package hugo.yml.corrector.markdown;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static hugo.yml.corrector.markdown.FileContent.FIELDS_DELIMITER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class IsYmlLinePredicateTest {

    FileContent.IsYmlLinePredicate predicate;

    @Before
    public void setUp() {
        predicate = new FileContent.IsYmlLinePredicate();
    }

    @Test
    public void betweenDelimiterLinesArePassed() {
        final String srcText = "some src text";

        String inText = FIELDS_DELIMITER + '\n'
                + srcText + '\n'
                + FIELDS_DELIMITER;

        List<String> foundStrs = inText.lines().filter(predicate).collect(Collectors.toUnmodifiableList());

        assertThat(foundStrs.size(), is(1));
        assertThat(foundStrs.get(0), is(srcText));
    }

    @Test
    public void outOfDelimitersLinesAreProcessed() {
        final String srcText = "body doc text";

        String inText = FIELDS_DELIMITER + '\n'
                + """
                some
                YAML
                text
                """
                + FIELDS_DELIMITER + '\n'
                + srcText;

        List<String> foundStrs = inText.lines()
                .filter(predicate.negate())
                .filter(line -> !line.startsWith(FIELDS_DELIMITER))
                .collect(Collectors.toUnmodifiableList());

        assertThat(foundStrs.size(), is(1));
        assertThat(foundStrs.get(0), is(srcText));
    }
}
