/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

import java.text.NumberFormat;
import java.util.Locale;

import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Helper class mainly used for UI display
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaDisplayValue {

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    protected static final long KB_LIMIT = 1024L;

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    protected static final long MB_LIMIT = 1024L * KB_LIMIT;

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    protected static final long GB_LIMIT = 1024L * MB_LIMIT;

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static final String GB_UNIT = "label.unit.GB";

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static final String MB_UNIT = "label.unit.MB";

    /** @deprecated since 2025.11 */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static final String KB_UNIT = "label.unit.KB";

    public static final String UNLIMITED_VALUE = "label.unit.unlimited.value";

    protected final ByteSize byteSize;

    public QuotaDisplayValue(ByteSize byteSize) {
        this.byteSize = byteSize;
    }

    /**
     * @param value the quota size in {@link ByteSize.Unit#KIBI KiB} unit
     */
    public QuotaDisplayValue(long value) {
        this(ByteSize.ofKibibytes(value));
    }

    /** @deprecated since 2025.11, use {@link #QuotaDisplayValue(long)} instead */
    @Deprecated(since = "2025.11", forRemoval = true)
    public QuotaDisplayValue(long value, long max) {
        this(value);
    }

    /** @since 2025.11 */
    public ByteSize getByteSize() {
        return byteSize;
    }

    /**
     * @return the current quota value in {@link ByteSize.Unit#KIBI KiB}
     */
    public long getValue() {
        return byteSize.toKibibytes();
    }

    /** @deprecated since 2025.11, use {@link #getByteSize()} instead */
    @Deprecated(since = "2025.11", forRemoval = true)
    public float getValueInUnit() {
        var value = byteSize.toKibibytes();
        float valueInUnit;
        if (value < 0) {
            valueInUnit = 0;
        } else if (value > GB_LIMIT) {
            valueInUnit = Float.valueOf(value) / GB_LIMIT;
        } else if (value > MB_LIMIT) {
            valueInUnit = Float.valueOf(value) / MB_LIMIT;
        } else {
            valueInUnit = Float.valueOf(value) / KB_LIMIT;
        }
        return valueInUnit;
    }

    /** @deprecated since 2025.11, use {@link #getByteSize()} instead */
    @Deprecated(since = "2025.11", forRemoval = true)
    public String getUnit() {
        var value = byteSize.toKibibytes();
        String unit;
        if (value < 0) {
            unit = UNLIMITED_VALUE;
        } else if (value > GB_LIMIT) {
            unit = GB_UNIT;
        } else if (value > MB_LIMIT) {
            unit = MB_UNIT;
        } else {
            unit = KB_UNIT;
        }
        return unit;
    }

    /** @since 2025.11 */
    public String format(Locale locale) {
        ByteSize.Unit unit;
        long byteSizeLong = byteSize.toBytes();
        if (byteSizeLong > ByteSize.Unit.GIBI.toBytes(1)) {
            unit = ByteSize.Unit.GIBI;
        } else if (byteSizeLong > ByteSize.Unit.MEBI.toBytes(1)) {
            unit = ByteSize.Unit.MEBI;
        } else {
            unit = ByteSize.Unit.KIBI;
        }
        var numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(unit.fromBytes(byteSize.toBytes())) + " "
                + I18NUtils.getMessageString("messages", unit.labelKey(), null, locale);
    }

    /** @deprecated since 2025.11, not used */
    @Deprecated(since = "2025.11", forRemoval = true)
    public String getPercent() {
        return "";
    }
}
