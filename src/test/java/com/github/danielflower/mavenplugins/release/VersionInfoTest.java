package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class VersionInfoTest {

    @Test
    public void compareEqual() {
        compare(2L, 1L, 2L, 1L, 0);
    }

    @Test
    public void compareEqualAllNull() {
        compare(null, null, null, null, 0);
    }

    @Test
    public void compareBugfixNumberNull() {
        compare(null, 1L, null, 2L, -1);
    }

    @Test
    public void compareEqualBugfixNumberNull() {
        compare(null, 1L, null, 1L, 0);
    }

    @Test
    public void compareEqualBuildnumberNull() {
        compare(2L, null, 2L, null, 0);
    }


    @Test
    public void compareBiggerBuildNumber() {
        compare(2L, 2L, 2L, 1L, 1);
    }

    @Test
    public void compareBiggerBranchNumber() {
        compare(3L, 1L, 2L, 1L, 1);
    }

    @Test
    public void compareOtherBranchNull() {
        compare(2L, 1L, null, 1L, 1);
    }

    @Test
    public void compareOtherBuildNull() {
        compare(2L, 1L, 2L, null, 1);
    }

    @Test
    public void branchNumberSucceeds() {
        compare(2L, 1L, 1L, 4L, 1);
    }

    private void compare(Long branchOne, Long buildOne, Long branchTwo, Long buildTwo, int expected) {
        final VersionInfoImpl infoOne = new VersionInfoImpl(branchOne, buildOne);
        final VersionInfoImpl infoTwo = new VersionInfoImpl(branchTwo, buildTwo);
        assertThat(infoOne.compareTo(infoTwo), is(expected));
        assertThat(infoTwo.compareTo(infoOne), is(-expected));
    }


}