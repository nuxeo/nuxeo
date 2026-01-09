/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.metrics.reporter.patch;

import java.util.Locale;
import java.util.Map;

/**
 * Builds metric payload lines in StatsD / DogStatsD protocol format.
 * <p>
 * Format: {@code metric.name:value|g|#dim1:val1,dim2:val2}
 *
 * @since 2025.15
 */
public class StatsDPayloadBuilder {

    private final String prefix;

    private final String hostname;

    private final Map<String, String> dimensions;

    public StatsDPayloadBuilder(String prefix, String hostname, Map<String, String> dimensions) {
        this.prefix = prefix;
        this.hostname = hostname;
        this.dimensions = dimensions != null ? dimensions : Map.of();
    }

    /**
     * Appends a single metric line to the given StringBuilder in StatsD / DogStatsD format.
     */
    public void appendMetricLine(StringBuilder sb, String metricName, Map<String, String> metricTags, double value) {
        // Skip NaN and Infinite values as they are not valid for StatsD
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return;
        }
        // Metric name with prefix, sanitized for StatsD
        sb.append(sanitizeMetricName(prefix)).append('.').append(sanitizeMetricName(metricName));

        // Value and type (always gauge for simplicity)
        sb.append(':').append(formatValue(value)).append("|g");

        // Dimensions in DogStatsD format: |#key1:val1,key2:val2
        sb.append("|#");
        boolean first = true;

        // Add host dimension first
        if (hostname != null && !hostname.isEmpty()) {
            sb.append("host:").append(sanitize(hostname));
            first = false;
        }

        // Add global dimensions
        for (Map.Entry<String, String> dim : dimensions.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append(sanitize(dim.getKey())).append(':').append(sanitize(dim.getValue()));
            first = false;
        }

        // Add metric-specific tags
        if (metricTags != null) {
            for (Map.Entry<String, String> tag : metricTags.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(sanitize(tag.getKey())).append(':').append(sanitize(tag.getValue()));
                first = false;
            }
        }

        sb.append('\n');
    }

    /**
     * Sanitizes metric names for StatsD protocol.
     * <p>
     * Common rules: letters, numbers, underscores, hyphens, dots (1-250 chars), must start with a letter.
     */
    public static String sanitizeMetricName(String s) {
        if (s == null || s.isEmpty()) {
            return "unknown";
        }
        // Replace characters not allowed in metric names
        // Keep: a-z, A-Z, 0-9, _, -, .
        String result = s.replaceAll("[^a-zA-Z0-9_.-]", "_");

        // Collapse multiple underscores
        result = result.replaceAll("_{2,}", "_");

        // Must start with a letter
        if (!result.isEmpty() && !Character.isLetter(result.charAt(0))) {
            result = "m" + result;
        }

        // Limit length
        if (result.length() > 250) {
            result = result.substring(0, 250);
        }

        return result.isEmpty() ? "unknown" : result;
    }

    /**
     * Sanitizes dimension/tag keys and values for StatsD protocol.
     * <p>
     * Common rules: letters, numbers, underscores, hyphens, dots, slashes; cannot start with hyphen, dot, or slash;
     * colons are reserved as key:value separators.
     */
    public static String sanitize(String s) {
        if (s == null || s.isEmpty()) {
            return "unknown";
        }
        // Replace characters not allowed in dimensions/tags
        // Keep: a-z, A-Z, 0-9, _, -, ., /
        // Replace: spaces, colons, and other special chars with underscore
        String result = s.replaceAll("[^a-zA-Z0-9_./-]", "_");

        // Collapse multiple underscores
        result = result.replaceAll("_{2,}", "_");

        // Handle empty result after sanitization
        if (result.isEmpty()) {
            return "unknown";
        }

        // Cannot start with hyphen, dot, or slash
        if (result.charAt(0) == '-' || result.charAt(0) == '.' || result.charAt(0) == '/') {
            result = "_" + result.substring(1);
        }

        // Limit length for dimension values
        if (result.length() > 255) {
            result = result.substring(0, 255);
        }

        return result;
    }

    /**
     * Formats a numeric value for the metric payload (integer when possible, otherwise decimal with trailing zeros
     * removed).
     */
    public static String formatValue(double value) {
        // Use integer format if possible to reduce payload size
        if (value == (long) value && Math.abs(value) < Long.MAX_VALUE) {
            return String.valueOf((long) value);
        }
        // Use up to 6 decimal places, removing trailing zeros
        // Use Locale.ROOT to ensure dot as decimal separator regardless of system locale
        String formatted = String.format(Locale.ROOT, "%.6f", value);
        // Remove trailing zeros after decimal point
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return formatted;
    }
}
