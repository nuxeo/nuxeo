package org.nuxeo.ecm.directory.ldap.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * </p>This class is used to apply corrections to a filter expression. It is
 * mainly used for filters that come from an Active Directory.</p>
 *
 * <p>
 * You can apply the corrections you want, in the desired order. The available
 * jobs are listed in the enum FilterJobs.
 * </p>
 *
 * <p>
 * Example: <code>FilterExpressionCorrector.correctFilter(filterValue,
                            FilterJobs.JOB1, FilterJobs.JOB2);</code>
 * </p>
 *
 * @author Nicolas Ulrich <nulrich@nuxeo.com>
 *
 */
public class FilterExpressionCorrector {

    /**
     * Enumeration of the available jobs
     */
    public enum FilterJobs {

        CORRECT_NOT(notJob);

        private final ICorrectorJob job;

        FilterJobs(final ICorrectorJob job) {
            this.job = job;
        }

        protected String run(final String filter) {
            return job.run(filter);
        }
    }

    /**
     * A Job interface
     */
    interface ICorrectorJob {
        String run(final String filter);
    }

    /**
     * This job find "!expression" and replace it by "!(expression)"
     */
    private static ICorrectorJob notJob = new ICorrectorJob() {

        public String run(final String filter) {

            final StringBuffer newString = new StringBuffer();

            // Find 'not' without parenthesis
            final Pattern pattern = Pattern.compile("![^(][^\\)]+[)]");
            final Matcher matcher = pattern.matcher(filter);

            // Find all the matches.
            while (matcher.find()) {

                // Add parenthesis
                final StringBuffer res = new StringBuffer(matcher.group());
                res.insert(1, '(').append(')');

                // Replace
                matcher.appendReplacement(newString, res.toString());
            }

            matcher.appendTail(newString);

            return newString.toString();

        }

    };

    /**
     * Apply the chosen Correctors to the filter expression
     *
     * @param filterExpresion The filter expression to correct
     * @param jobs List of the jobs you want to apply.
     * @return
     */
    public static String correctFilter(final String filterExpresion,
            final FilterJobs... jobs) {

        String result = filterExpresion;

        for (FilterJobs job : jobs) {
            result = job.run(result);
        }

        return result;
    }

}
