package com.github.danielflower.mavenplugins.release;

/**
 * Build number consisting of "generic build number",
 */
public final class VersionInfo implements Comparable<VersionInfo>{
    private final Long buildNumber;
    private final Long releaseNumber;

    public VersionInfo(Long buildNumber) {
        this(buildNumber, null);
    }

    public VersionInfo(Long buildNumber, Long bugfixBranchNumber) {
        this.buildNumber = buildNumber;
        this.releaseNumber = bugfixBranchNumber;
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
        return releaseNumber;
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
        return releaseNumber != null
               ? releaseNumber.equals(that.releaseNumber)
               : that.releaseNumber == null;
    }

    @Override
    public int hashCode() {
        int result = buildNumber != null
                     ? buildNumber.hashCode()
                     : 0;
        result = 31 * result + (releaseNumber != null
                                ? releaseNumber.hashCode()
                                : 0);
        return result;
    }

    @Override
    public String toString() {
        if (releaseNumber == null) {
            return buildNumber + "";
        } else {
            return releaseNumber + "." + buildNumber;
        }
    }

    @Override
    public int compareTo(VersionInfo o) {
        if (this.equals(o)) {
            return 0;
        }
        if (releaseNumber == null) {
            if (o.releaseNumber == null) {
                return compareLong(this.buildNumber, o.buildNumber);
            } else {
                return -1;
            }
        } else {
            if (o.releaseNumber == null) {
                return 1;
            } else {
                if (releaseNumber.equals(o.releaseNumber)) {
                    return compareLong(this.buildNumber, o.buildNumber);
                } else {
                    return compareLong(releaseNumber, o.releaseNumber);
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
