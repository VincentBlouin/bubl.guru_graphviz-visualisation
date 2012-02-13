package org.triple_brain.module.graphviz_visualisation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vincent Blouin
 */
public class StringScanner {

    private Pattern currentPattern;
    private StringBuffer remainingText = new StringBuffer("");
    private StringBuffer lastRemovedText = new StringBuffer("");

    public static StringScanner withTextToParseAndCurrentPattern(String remainingText, Pattern pattern) {
        return new StringScanner(remainingText, pattern);
    }

    private StringScanner(String remainingText, Pattern pattern) {
        this.currentPattern = pattern;
        this.remainingText = new StringBuffer(remainingText);
    }

    /*
    * Calls Scanner.next() a number of times
    * @param howManyTimesNext how many times to call the next method of the same class
    * @return the occurrence matching the currentPattern for the last next() call
     */
    public String next(Integer howManyTimesNext) {
        for (int i = 1; i < howManyTimesNext; i++) {
            next();
        }
        return next();
    }

    /*
    * Removes the text found before the currentPattern of the remainingText. Affects the text found before the remainingPattern before the remainingText
    * @return the next occurrence of the currentPattern within the remainingText or an empty string if not found
    */
    public String next() {
        Matcher result = currentPattern.matcher(remainingText);
        if (result.find()) {
            //reinitializing the buffers because result uses an append method
            lastRemovedText = new StringBuffer();
            remainingText = new StringBuffer();
            result.appendReplacement(lastRemovedText, "");
            result.appendTail(remainingText);
            return result.group(0);
        } else {
            return "";
        }
    }

    public String lastRemovedText() {
        return lastRemovedText.toString();
    }

    public String remainingText() {
        return remainingText.toString();
    }

    public StringScanner pattern(Pattern pattern) {
        this.currentPattern = pattern;
        return this;
    }
}
