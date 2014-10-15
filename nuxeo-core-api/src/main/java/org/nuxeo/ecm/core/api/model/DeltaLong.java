/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

/**
 * Value holding a base {@link Long} value and a delta.
 * <p>
 * This is used when the actual intent of the value is to be an incremental
 * update to an existing value.
 *
 * @since 6.0
 */
public class DeltaLong extends Delta {

    private static final long serialVersionUID = 1L;

    private final long base;

    private final long delta;

    /**
     * A {@link DeltaLong} with the given base and delta.
     */
    public DeltaLong(long base, long delta) {
        this.base = base;
        this.delta = delta;
    }

    /**
     * Constructs a {@link DeltaLong} from the given base number and delta, or a
     * {@link Long} if the base is {@code null}.
     * <p>
     * The base number may be a {@link Long} or a {@link DeltaLong}. If it is a
     * {@link DeltaLong} then the returned value will keep its base and just add
     * deltas.
     *
     * @param base the base number
     * @param delta the delta
     * @return a new {@link DeltaLong} or {@link Long}
     */
    public static Number deltaOrLong(Number base, long delta) {
        if (base == null) {
            return Long.valueOf(delta);
        } else if (base instanceof Long) {
            return new DeltaLong(base.longValue(), delta);
        } else if (base instanceof DeltaLong) {
            DeltaLong dl = (DeltaLong) base;
            if (delta == 0) {
                return dl;
            } else {
                return new DeltaLong(dl.getBase(), dl.getDelta() + delta);
            }
        } else {
            throw new IllegalArgumentException(base.getClass().getName());
        }
    }

    @Override
    public Delta add(Delta other) {
        if (!(other instanceof DeltaLong)) {
            throw new IllegalArgumentException("Cannot add "
                    + getClass().getSimpleName() + " and "
                    + other.getClass().getSimpleName());
        }
        return new DeltaLong(base, delta + ((DeltaLong) other).delta);
    }

    // @Override
    public long getBase() {
        return base;
    }

    // @Override
    public long getDelta() {
        return delta;
    }

    @Override
    public Long getDeltaValue() {
        return Long.valueOf(delta);
    }

    @Override
    public Long getFullValue() {
        return Long.valueOf(longValue());
    }

    @Override
    public long longValue() {
        return base + delta;
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public float floatValue() {
        return (float) longValue();
    }

    @Override
    public double doubleValue() {
        return (double) longValue();
    }

    @Override
    public String toString() {
        return Long.toString(longValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeltaLong) {
            DeltaLong dl = (DeltaLong) obj;
            return base == dl.base && delta == dl.delta;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31 + (int) (base ^ (base >>> 32));
        return 31 * result + (int) (delta ^ (delta >>> 32));
    }

}
