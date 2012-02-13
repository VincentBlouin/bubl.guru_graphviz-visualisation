package org.triple_brain.module.graphviz_visualisation;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Vincent Blouin
 */
public class StringScannerTest {

    @Test
    public void can_get_next_occurrence_of_pattern(){
        String textToScan = "bonjour bb 123 hello";
        Pattern testPattern = Pattern.compile("bb");
        StringScanner scanner = StringScanner.withTextToParseAndCurrentPattern(textToScan, testPattern);

		assertThat(scanner.next(),is("bb"));
        assertThat(scanner.remainingText().trim(),is("123 hello"));
        assertThat(scanner.lastRemovedText().trim(),is("bonjour"));

        scanner.pattern(Pattern.compile("[0-9]+"));
        assertThat(scanner.next(),is("123"));
    }

}
