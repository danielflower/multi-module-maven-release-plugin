package scaffolding;

import org.hamcrest.*;

import java.util.List;

public class ExactCountMatcher extends TypeSafeDiagnosingMatcher<List<String>> {
    private final Matcher<String> stringMatcher;
    private int expectedCount;

    private ExactCountMatcher(Matcher<String> stringMatcher, int expectedCount) {
        this.stringMatcher = stringMatcher;
        this.expectedCount = expectedCount;
    }

    @Override
    protected boolean matchesSafely(List<String> items, Description mismatchDescriptor) {
        int count = 0;
        for (String item : items) {
            if (stringMatcher.matches(item)) {
                count++;
            }
        }
        boolean okay = count == expectedCount;
        if (!okay) {
            mismatchDescriptor.appendText("was matched " + count + " times");
        }
        return okay;
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(stringMatcher).appendText(" " + expectedCount + " times");
    }

    @Factory
    public static Matcher<? super List<String>> noneOf(Matcher<String> stringMatcher) {
        return new ExactCountMatcher(stringMatcher, 0);
    }

    @Factory
    public static Matcher<? super List<String>> oneOf(Matcher<String> stringMatcher) {
        return new ExactCountMatcher(stringMatcher, 1);
    }

    @Factory
    public static Matcher<? super List<String>> twoOf(Matcher<String> stringMatcher) {
        return new ExactCountMatcher(stringMatcher, 2);
    }

    @Factory
    public static Matcher<? super List<String>> threeOf(Matcher<String> stringMatcher) {
        return new ExactCountMatcher(stringMatcher, 3);
    }
}
