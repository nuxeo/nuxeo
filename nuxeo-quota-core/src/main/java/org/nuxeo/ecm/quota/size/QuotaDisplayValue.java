/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
            valueInUnit = new Float(value) / GB_LIMIT;
        } else if (value > MB_LIMIT) {
            unit = MB_UNIT;
            valueInUnit = new Float(value) / MB_LIMIT;
        } else {
            unit = KB_UNIT;
            valueInUnit = new Float(value) / KB_LIMIT;
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
            return formatter.format((new Float(value) / max) * 100) + "%";
        } else {
            return "";
        }
    }
}
