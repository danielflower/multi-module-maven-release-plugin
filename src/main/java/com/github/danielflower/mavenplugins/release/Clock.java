package com.github.danielflower.mavenplugins.release;

import java.util.Date;

public interface Clock {

    Date now();


    public static Clock SystemClock = new Clock() {
        @Override
        public Date now() {
            return new Date();
        }
    };
}
