/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.model;

/**
 * Value holding a base {@link Long} value and a delta.
 * <p>
 * This is used when the actual intent of the value is to be an incremental update to an existing value.
 *
 * @since 6.0
 */
public class DeltaLong extends Delta {

    private static final long serialVersionUID = 1L;

    private final Long base;

    private final long delta;

    /**
     * A {@link DeltaLong} with the given base and delta.
     */
    public DeltaLong(Long base, long delta) {
        this.base = base;
        this.delta = delta;
    }

    /**
     * A {@link DeltaLong} with the given base and delta.
     *
     * @deprecated since 8.3, use {@link #DeltaLong(Long, long)} instead.
     */
    @Deprecated
    public DeltaLong(long base, long delta) {
        this.base = Long.valueOf(base);
        this.delta = delta;
    }

    /**
     * Returns a {@link DeltaLong} from the given base number and delta.
     * <p>
     * The base number may be a {@link Long} (which may be null), or a {@link DeltaLong}. If it is a {@link DeltaLong}
     * then the returned value will keep its base and just add deltas.
     *
     * @param base the base number
     * @param delta the delta
     * @return a {@link DeltaLong}
     */
    public static DeltaLong valueOf(Number base, long delta) {
        if (base == null || base instanceof Long) {
            return new DeltaLong((Long) base, delta);
        } else if (base instanceof DeltaLong) {
            DeltaLong dl = (DeltaLong) base;
            if (delta == 0) {
                return dl;
            } else {
                return new DeltaLong(dl.base, dl.delta + delta);
            }
        } else {
            throw new IllegalArgumentException(base.getClass().getName());
        }
    }

    /**
     * Returns a {@link DeltaLong} from the given base number and delta.
     * <p>
     * The base number may be a {@link Long} (which may be null), or a {@link DeltaLong}. If it is a {@link DeltaLong}
     * then the returned value will keep its base and just add deltas.
     *
     * @param base the base number
     * @param delta the delta
     * @return a {@link DeltaLong}
     * @deprecated since 8.3, use {@link #valueOf(Number, long)} instead.
     */
    @Deprecated
    public static DeltaLong deltaOrLong(Number base, long delta) {
        return valueOf(base, delta);
    }

    @Override
    public Delta add(Delta other) {
        if (!(other instanceof DeltaLong)) {
            throw new IllegalArgumentException("Cannot add " + getClass().getSimpleName() + " and "
                    + other.getClass().getSimpleName());
        }
        return new DeltaLong(base, delta + ((DeltaLong) other).delta);
    }

    @Override
    public Number add(Number other) {
        if (!(other instanceof Long)) {
            throw new IllegalArgumentException("Cannot add " + getClass().getSimpleName() + " and "
                    + other.getClass().getSimpleName());
        }
        return Long.valueOf(((Long) other).longValue() + delta);
    }

    @Override
    public Long getBase() {
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
        return base == null ? delta : base.longValue() + delta;
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public float floatValue() {
        return longValue();
    }

    @Override
    public double doubleValue() {
        return longValue();
    }

    @Override
    public String toString() {
        return Long.toString(longValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeltaLong) {
            DeltaLong dl = (DeltaLong) obj;
            if (delta != dl.delta) {
                return false;
            }
            return base == null ? dl.base == null : base.equals(dl.base);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 31;
        if (base != null) {
            long b = base.longValue();
            result += (int) (b ^ (b >>> 32));
        }
        return 31 * result + (int) (delta ^ (delta >>> 32));
    }

}
