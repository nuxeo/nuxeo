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

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Helper class mainly used for UI display
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaDisplayValue {

    protected static final long KB_LIMIT = 1024L;

    protected static final long MB_LIMIT = 1024L * KB_LIMIT;

    protected static final long GB_LIMIT = 1024L * MB_LIMIT;

    public static final String GB_UNIT = "label.unit.GB";

    public static final String MB_UNIT = "label.unit.MB";

    public static final String KB_UNIT = "label.unit.KB";

    public static final String UNLIMITED_VALUE = "label.unit.unlimited.value";

    protected final long value;

    protected float valueInUnit;

    protected String unit;

    protected long max;

    public QuotaDisplayValue(long value) {
        this.value = value;
        init();
    }

    public QuotaDisplayValue(long value, long max) {
        this(value);
        this.max = max;
    }

    protected void init() {
        if (value < 0) {
            unit = UNLIMITED_VALUE;
            valueInUnit = 0;
        } else if (value > GB_LIMIT) {
            unit = GB_UNIT;
            valueInUnit = Float.valueOf(value) / GB_LIMIT;
        } else if (value > MB_LIMIT) {
            unit = MB_UNIT;
            valueInUnit = Float.valueOf(value) / MB_LIMIT;
        } else {
            unit = KB_UNIT;
            valueInUnit = Float.valueOf(value) / KB_LIMIT;
        }
    }

    public long getValue() {
        return value;
    }

    public float getValueInUnit() {
        return valueInUnit;
    }

    public String getUnit() {
        return unit;
    }

    public String getPercent() {
        if (max > 0) {
            NumberFormat formatter = new DecimalFormat("0.0");
            return formatter.format((Float.valueOf(value) / max) * 100) + "%";
        } else {
            return "";
        }
    }
}
