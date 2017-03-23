package com.github.danielflower.mavenplugins.release;

/**
 * Build number consisting of "generic build number",
 */
public final class VersionInfo implements Comparable<VersionInfo>{
    private final Long buildNumber;
    private final Long bugfixBranchNumber;

    public VersionInfo(Long buildNumber) {
        this(null, buildNumber);
    }

    public VersionInfo(Long bugfixBranchNumber, Long buildNumber) {
        this.buildNumber = buildNumber;
        this.bugfixBranchNumber = bugfixBranchNumber;
    }

    /**
     * @return the last number of the automatically created version.
     */
    public Long getBuildNumber() {
        return buildNumber;
    }

    /**
     * @return the number before the build number. Used for bugfix releases.
     */
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

        VersionInfo that = (VersionInfo) o;

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
    public int compareTo(VersionInfo o) {
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
