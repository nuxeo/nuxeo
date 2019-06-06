/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.sql;

import java.time.format.DateTimeParseException;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.nuxeo.common.utils.PeriodAndDuration;
import org.nuxeo.runtime.api.Framework;

/**
 * This defines the constants for NXQL queries.
 *
 * @author Florent Guillaume
 */
public class NXQL {

    /**
     * Property containing the test value of the current date for {@link #nowPlusPeriodAndDuration}.
     * <p>
     * Only for tests, not a public API.
     *
     * @since 11.1
     */
    public static final String TEST_NXQL_NOW = "test.nxql.now";

    // constant utility class
    private NXQL() {
    }

    /** The NXQL query type. */
    public static final String NXQL = "NXQL";

    public static final String ECM_PREFIX = "ecm:";

    public static final String ECM_UUID = "ecm:uuid";

    public static final String ECM_PATH = "ecm:path";

    public static final String ECM_NAME = "ecm:name";

    public static final String ECM_POS = "ecm:pos";

    public static final String ECM_PARENTID = "ecm:parentId";

    public static final String ECM_MIXINTYPE = "ecm:mixinType";

    public static final String ECM_PRIMARYTYPE = "ecm:primaryType";

    public static final String ECM_ISPROXY = "ecm:isProxy";

    public static final String ECM_ISVERSION = "ecm:isVersion";

    /**
     * @since 5.7.3
     */
    public static final String ECM_ISVERSION_OLD = "ecm:isCheckedInVersion";

    public static final String ECM_LIFECYCLESTATE = "ecm:currentLifeCycleState";

    public static final String ECM_VERSIONLABEL = "ecm:versionLabel";

    public static final String ECM_FULLTEXT = "ecm:fulltext";

    public static final String ECM_FULLTEXT_JOBID = "ecm:fulltextJobId";

    /**
     * @since 6.0
     */
    public static final String ECM_FULLTEXT_SCORE = "ecm:fulltextScore";

    /**
     * @since 5.4.2
     */
    public static final String ECM_LOCK_OWNER = "ecm:lockOwner";

    /**
     * @since 5.4.2
     */
    public static final String ECM_LOCK_CREATED = "ecm:lockCreated";

    /**
     * @since 5.7
     */
    public static final String ECM_TAG = "ecm:tag";

    /**
     * @since 5.7
     */
    public static final String ECM_PROXY_TARGETID = "ecm:proxyTargetId";

    /**
     * @since 5.7
     */
    public static final String ECM_PROXY_VERSIONABLEID = "ecm:proxyVersionableId";

    /**
     * @since 5.7.3
     */
    public static final String ECM_ISCHECKEDIN = "ecm:isCheckedIn";

    /**
     * @since 5.7.3
     */
    public static final String ECM_ISLATESTVERSION = "ecm:isLatestVersion";

    /**
     * @since 5.7.3
     */
    public static final String ECM_ISLATESTMAJORVERSION = "ecm:isLatestMajorVersion";

    /**
     * @since 5.7.3
     */
    public static final String ECM_VERSIONCREATED = "ecm:versionCreated";

    /**
     * @since 5.7.3
     */
    public static final String ECM_VERSIONDESCRIPTION = "ecm:versionDescription";

    /**
     * @since 5.7.3
     */
    public static final String ECM_VERSION_VERSIONABLEID = "ecm:versionVersionableId";

    /**
     * @since 6.0
     */
    public static final String ECM_ANCESTORID = "ecm:ancestorId";

    /**
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL = "ecm:acl";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /principal}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_PRINCIPAL = "principal";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /permission}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_PERMISSION = "permission";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /grant}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_GRANT = "grant";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /name}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_NAME = "name";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /pos}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_POS = "pos";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /creator}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_CREATOR = "creator";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /begin}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_BEGIN = "begin";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /end}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_END = "end";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /status}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_STATUS = "status";

    /**
     * @since 10.1
     */
    public static final String ECM_ISTRASHED = "ecm:isTrashed";

    /** @since 11.1 */
    public static final String ECM_ISRECORD = "ecm:isRecord";

    /** @since 11.1 */
    public static final String ECM_RETAINUNTIL = "ecm:retainUntil";

    /** @since 11.1 */
    public static final String ECM_HASLEGALHOLD = "ecm:hasLegalHold";

    /**
     * The function returning the current datetime. It can optionally have as an argument a duration that will be added
     * to the current datetime, expressed as a ISO 8601 period.
     * <p>
     * See {@link PeriodAndDuration#parse} for the exact format.
     *
     * @see PeriodAndDuration#parse
     * @since 11.1
     */
    public static final String NOW_FUNCTION = "NOW";

    /**
     * Escapes a string into a single-quoted string for NXQL.
     * <p>
     * Any single quote or backslash characters are escaped with a backslash.
     *
     * @param s the string to escape
     * @return the escaped string
     * @since 5.7, 5.6.0-HF08
     */
    public static String escapeString(String s) {
        return "'" + escapeStringInner(s) + "'";
    }

    /**
     * Escapes a string (assumed to be single quoted) for NXQL.
     * <p>
     * Any single quote or backslash characters are escaped with a backslash.
     *
     * @param s the string to escape
     * @return the escaped string, without external quotes
     * @since 5.7, 5.6.0-HF08
     */
    public static String escapeStringInner(String s) {
        // backslash -> backslash backslash
        // quote -> backslash quote
        // newline -> backslash n
        return s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'").replaceAll("\n", "\\\\n");
    }

    /**
     * Returns the current dateTime to which the period and duration (if present) has been added.
     *
     * @param periodAndDurationText the period and duration as text, or {@code null}
     * @return the current dateTime to which the period and duration has been added
     * @throws IllegalArgumentException if the period and duration cannot be parsed
     * @since 11.1
     */
    // also used by NxqlQueryConverter
    public static DateTime nowPlusPeriodAndDuration(String periodAndDurationText) {
        DateTime now;
        if (Framework.getProperty(TEST_NXQL_NOW) != null) {
            now = DateTime.parse(Framework.getProperty(TEST_NXQL_NOW));
        } else {
            now = new DateTime(ISOChronology.getInstanceUTC());
        }
        if (periodAndDurationText == null) {
            return now;
        } else {
            return addPeriondAndDuration(now, periodAndDurationText);
        }
    }

    /**
     * Adds to the given dateTime the period and duration, expressed as text.
     *
     * @param dateTime the dateTime
     * @param text the period and duration, as text
     * @return the dateTime to which the period and duration has been added
     * @throws IllegalArgumentException if the period and duration cannot be parsed
     * @since 11.1
     */
    // public for tests
    public static DateTime addPeriondAndDuration(DateTime dateTime, String text) {
        PeriodAndDuration pd;
        try {
            pd = PeriodAndDuration.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid period: " + text, e);
        }
        return dateTime.plusYears(pd.period.getYears())
                       .plusMonths(pd.period.getMonths())
                       .plusDays(pd.period.getDays())
                       .plus(org.joda.time.Duration.millis(pd.duration.toMillis()));
    }

}
