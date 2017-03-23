package com.github.danielflower.mavenplugins.release;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class VersionInfoTest {

    @Test
    public void compareEqual() {
        compare(1L, 2L, 1L, 2L, 0);
    }

    @Test
    public void compareEqualAllNull() {
        compare(null, null, null, null, 0);
    }

    @Test
    public void compareEqualBugfixNumberNull() {
        compare(1L, null, 1L, null, 0);
    }

    @Test
    public void compareEqualBuildnumberNull() {
        compare(null, 2L, null, 2L, 0);
    }


    @Test
    public void compareBiggerBuildNumber() {
        compare(2L, 2L, 1L, 2L, 1);
    }

    @Test
    public void compareBiggerBranchNumber() {
        compare(1L, 3L, 1L, 2L, 1);
    }

    @Test
    public void compareOtherBranchNull() {
        compare(1L, 2L, 1L, null, 1);
    }

    @Test
    public void compareOtherBuildNull() {
        compare(1L, 2L, null, 2L, 1);
    }

    private void compare(Long buildOne, Long branchOne, Long buildTwo, Long branchTwo, int exptected) {
        final VersionInfo infoOne = new VersionInfo(buildOne, branchOne);
        final VersionInfo infoTwo = new VersionInfo(buildTwo, branchTwo);
        assertThat(infoOne.compareTo(infoTwo), is(exptected));
        assertThat(infoTwo.compareTo(infoOne), is(-exptected));
    }


}