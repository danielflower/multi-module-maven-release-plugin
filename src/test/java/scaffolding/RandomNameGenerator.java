package scaffolding;

import org.apache.commons.lang.RandomStringUtils;

public class RandomNameGenerator {
    public String randomName() {
        return RandomStringUtils.random(8, "abcdefghijklmnop012345678");
    }
}
