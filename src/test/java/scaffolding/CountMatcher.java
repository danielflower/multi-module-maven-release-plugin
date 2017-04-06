package scaffolding;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class CountMatcher extends TypeSafeDiagnosingMatcher<List<String>> {
    private final Matcher<String> stringMatcher;
    private final int             minCount;
    private final int             maxCount;

    private CountMatcher(Matcher<String> stringMatcher, int minCount, int maxCount) {
        this.stringMatcher = stringMatcher;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Factory
    public static Matcher<? super List<String>> noneOf(Matcher<String> stringMatcher) {
        return new CountMatcher(stringMatcher, 0, 0);
    }

    @Factory
    public static Matcher<? super List<String>> oneOf(Matcher<String> stringMatcher) {
        return new CountMatcher(stringMatcher, 1, 1);
    }

    @Factory
    public static Matcher<? super List<String>> atLeastOneOf(Matcher<String> stringMatcher) {
        return new CountMatcher(stringMatcher, 1, Integer.MAX_VALUE);
    }

    @Factory
    public static Matcher<? super List<String>> twoOf(Matcher<String> stringMatcher) {
        return new CountMatcher(stringMatcher, 2, 2);
    }

    @Override
    protected boolean matchesSafely(List<String> items, Description mismatchDescriptor) {
        int count = 0;
        for (String item : items) {
            if (stringMatcher.matches(item)) {
                count++;
            }
        }
        boolean okay = (count >= minCount && count <=maxCount);
        if (!okay) {
            mismatchDescriptor.appendText("was matched " + count + " times in the following list:");
            String separator = String.format("%n") + "          ";
            mismatchDescriptor.appendValueList(separator, separator, "", items);
        }
        return okay;
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(stringMatcher).appendText(" " + minCount + " times");
    }
}
