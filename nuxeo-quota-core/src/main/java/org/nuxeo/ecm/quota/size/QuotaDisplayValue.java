package org.nuxeo.ecm.quota.size;

public class QuotaDisplayValue {

    protected static final long KB_LIMIT = 1024L;

    protected static final long MB_LIMIT = 1024L * KB_LIMIT;

    protected static final long GB_LIMIT = 1024L * MB_LIMIT;

    public static final String GB_UNIT = "label.unit.GB";

    public static final String MB_UNIT = "label.unit.GM";

    public static final String KB_UNIT = "label.unit.KB";

    public static final String UNLIMITED_VALUE = "label.unit.unlimited.value";

    protected final long value;

    protected float valueInUnit;

    protected String unit;

    public QuotaDisplayValue(long value) {
        this.value = value;
        init();
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

}
