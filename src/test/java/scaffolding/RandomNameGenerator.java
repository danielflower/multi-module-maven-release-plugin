package scaffolding;

import org.apache.commons.lang.RandomStringUtils;

public final class RandomNameGenerator {

    private RandomNameGenerator() {
    }

    private static RandomNameGenerator INSTANCE = new RandomNameGenerator();

    public static RandomNameGenerator getInstance() {
        return INSTANCE;
    }

    public String randomName() {
        return RandomStringUtils.random(8, "abcdefghijklmnop012345678");
    }
}
