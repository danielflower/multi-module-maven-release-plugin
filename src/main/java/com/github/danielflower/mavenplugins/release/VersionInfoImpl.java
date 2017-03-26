package com.github.danielflower.mavenplugins.release;

/**
 * Build number consisting of "generic build number",
 */
public final class VersionInfoImpl implements Comparable<VersionInfoImpl>, VersionInfo {
    private final Long buildNumber;
    private final Long bugfixBranchNumber;

    public VersionInfoImpl(Long buildNumber) {
        this(null, buildNumber);
    }

    public VersionInfoImpl(Long bugfixBranchNumber, Long buildNumber) {
        this.buildNumber = buildNumber;
        this.bugfixBranchNumber = bugfixBranchNumber;
    }

    @Override
    public Long getBuildNumber() {
        return buildNumber;
    }

    @Override
    public Long getBugfixBranchNumber() {
        return bugfixBranchNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VersionInfoImpl that = (VersionInfoImpl) o;

        if (buildNumber != null
            ? !buildNumber.equals(that.buildNumber)
            : that.buildNumber != null) {
            return false;
        }
        return bugfixBranchNumber != null
               ? bugfixBranchNumber.equals(that.bugfixBranchNumber)
               : that.bugfixBranchNumber == null;
    }

    @Override
    public int hashCode() {
        int result = buildNumber != null
                     ? buildNumber.hashCode()
                     : 0;
        result = 31 * result + (bugfixBranchNumber != null
                                ? bugfixBranchNumber.hashCode()
                                : 0);
        return result;
    }

    @Override
    public String toString() {
        if (bugfixBranchNumber == null) {
            return buildNumber + "";
        } else {
            return bugfixBranchNumber + "." + buildNumber;
        }
    }

    @Override
    public int compareTo(VersionInfoImpl o) {
        if (this.equals(o)) {
            return 0;
        }
        if (bugfixBranchNumber == null) {
            if (o.bugfixBranchNumber == null) {
                return compareLong(this.buildNumber, o.buildNumber);
            } else {
                return -1;
            }
        } else {
            if (o.bugfixBranchNumber == null) {
                return 1;
            } else {
                if (bugfixBranchNumber.equals(o.bugfixBranchNumber)) {
                    return compareLong(this.buildNumber, o.buildNumber);
                } else {
                    return compareLong(bugfixBranchNumber, o.bugfixBranchNumber);
                }
            }
        }
    }

    private static int compareLong(Long l1, Long l2) {
        if (l1 == null) {
            if (l2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (l2 == null) {
                return 1;
            } else {
                return l1.compareTo(l2);
            }
        }
    }
}
