/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @since 2025.11
 */
public record ByteSize(long bytes) {

    private static final Logger log = LogManager.getLogger(ByteSize.class);

    public static final Pattern BYTE_SIZE_SIMPLE_FORMAT = Pattern.compile("^(\\d+ ?)([bkmgtpBKMGTP])?i?[bB]?$");

    public static ByteSize ofBytes(long bytes) {
        return new ByteSize(bytes);
    }

    public static ByteSize ofKibibytes(long kibibytes) {
        return new ByteSize(Unit.KIBI.toBytes(kibibytes));
    }

    public static ByteSize ofMebibytes(long mebibytes) {
        return new ByteSize(Unit.MEBI.toBytes(mebibytes));
    }

    public static ByteSize ofGibibytes(long gibibytes) {
        return new ByteSize(Unit.GIBI.toBytes(gibibytes));
    }

    public static ByteSize ofTebibytes(long tebibytes) {
        return new ByteSize(Unit.TEBI.toBytes(tebibytes));
    }

    public static ByteSize ofPebibytes(long pebibytes) {
        return new ByteSize(Unit.PEBI.toBytes(pebibytes));
    }

    /**
     * Obtains a {@link ByteSize} from a text string such as {@code _g}, {@code _G}, {@code _GB} or {@code _GiB}, with
     * the following supported units: {@code b}, {@code k}, {@code m}, {@code g}, {@code t}, {@code p}.
     * <p>
     * A string without unit is parsed with the {@code b} unit.
     *
     * @throws NumberFormatException if the text cannot be parsed to a byte size
     */
    public static ByteSize parse(String value) {
        Matcher matcher = BYTE_SIZE_SIMPLE_FORMAT.matcher(value);
        if (matcher.matches()) {
            String sizeWithPotentialLeadingSpace = matcher.group(1);
            // @deprecated since 2025.11, remove space mechanism
            if (sizeWithPotentialLeadingSpace.endsWith(" ")) {
                sizeWithPotentialLeadingSpace = sizeWithPotentialLeadingSpace.substring(0,
                        sizeWithPotentialLeadingSpace.length() - 1);
                log.warn(
                        "Space between a byte size and its unit is deprecated, current: {} needs to be replaced by: {}",
                        value,
                        sizeWithPotentialLeadingSpace + value.substring(sizeWithPotentialLeadingSpace.length() + 1));
            }
            long amount = Long.parseLong(sizeWithPotentialLeadingSpace);
            String group2 = matcher.group(2);
            var unit = group2 == null ? Unit.DEFAULT : switch (group2) {
                case "b", "B" -> Unit.DEFAULT;
                case "k", "K" -> Unit.KIBI;
                case "m", "M" -> Unit.MEBI;
                case "g", "G" -> Unit.GIBI;
                case "t", "T" -> Unit.TEBI;
                case "p", "P" -> Unit.PEBI;
                default -> throw new NumberFormatException(
                        "Text cannot be parsed to a byte size: %s under unit %s".formatted(value, matcher.group(2)));
            };
            return new ByteSize(unit.toBytes(amount));
        }
        throw new NumberFormatException("Text cannot be parsed to a byte size: %s".formatted(value));
    }

    public long toBytes() {
        return bytes();
    }

    public long toKibibytes() {
        return bytes() / Unit.KIBI.bytes;
    }

    public long toMebibytes() {
        return bytes() / Unit.MEBI.bytes;
    }

    public long toGibibytes() {
        return bytes() / Unit.GIBI.bytes;
    }

    public long toTebibytes() {
        return bytes() / Unit.TEBI.bytes;
    }

    public long toPebibytes() {
        return bytes() / Unit.PEBI.bytes;
    }

    /**
     * Returns the {@link ByteSize} formatted with the bigger unit it can fit into, meaning for example:
     * 
     * <pre>
     * <code>
     * System.out.println(ByteSize.ofGibibytes(1).toString()); // 1GiB
     * System.out.println(ByteSize.ofMebibytes(1024).toString()); // 1GiB
     * System.out.println(ByteSize.ofMebibytes(1025).toString()); // 1025MiB
     * </code>
     * </pre>
     *
     * @return the {@link ByteSize} formatted with the bigger unit it can fit into
     */
    @Override
    public String toString() {
        return ByteSize.Formatter.INTERNATIONAL_SYSTEM_UNIT.format(this);
    }

    /** Units of byte size. */
    public enum Unit {

        DEFAULT(1L, "label.unit.B"), //
        KIBI(1024L, "label.unit.KiB"), //
        MEBI(1024L * 1024L, "label.unit.MiB"), //
        GIBI(1024L * 1024L * 1024L, "label.unit.GiB"), //
        TEBI(1024L * 1024L * 1024L * 1024L, "label.unit.TiB"), //
        PEBI(1024L * 1024L * 1024L * 1024L * 1024L, "label.unit.PiB");

        protected final long bytes;

        protected final String labelKey;

        Unit(long bytes, String labelKey) {
            this.bytes = bytes;
            this.labelKey = labelKey;
        }

        public long toBytes(long amount) {
            return amount * bytes;
        }

        public double fromBytes(long amount) {
            return (double) amount / bytes;
        }

        public String labelKey() {
            return labelKey;
        }
    }

    public interface Formatter {

        Formatter INTERNATIONAL_SYSTEM_UNIT = new SimpleFormatter("", "KiB", "MiB", "GiB", "TiB", "PiB");

        Formatter JVM_HEAP_SIZE = new SimpleFormatter("", "k", "m", "g", "t", "p");

        String format(ByteSize size);
    }

    public record SimpleFormatter(String byteUnit, String kibibyteUnit, String mebibyteUnit, String gibibyteUnit,
            String tebibyteUnit, String pebibyteUnit) implements Formatter {

        @Override
        public String format(ByteSize size) {
            long formattedBytes = size.toBytes();
            String unit = byteUnit();
            if (formattedBytes % 1024 == 0) {
                formattedBytes = Math.floorDiv(formattedBytes, 1024);
                unit = kibibyteUnit();
                if (formattedBytes % 1024 == 0) {
                    formattedBytes = Math.floorDiv(formattedBytes, 1024);
                    unit = mebibyteUnit();
                    if (formattedBytes % 1024 == 0) {
                        formattedBytes = Math.floorDiv(formattedBytes, 1024);
                        unit = gibibyteUnit();
                        if (formattedBytes % 1024 == 0) {
                            formattedBytes = Math.floorDiv(formattedBytes, 1024);
                            unit = tebibyteUnit();
                            if (formattedBytes % 1024 == 0) {
                                formattedBytes = Math.floorDiv(formattedBytes, 1024);
                                unit = pebibyteUnit();
                            }
                        }
                    }
                }
            }
            return formattedBytes + unit;
        }
    }
}
