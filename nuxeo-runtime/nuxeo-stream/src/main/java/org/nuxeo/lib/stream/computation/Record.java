/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.computation;

import static java.lang.Math.min;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Basic data object that contains: key, watermark, flag and data.
 *
 * @since 9.3
 */
@SuppressWarnings("deprecation")
public class Record implements Externalizable {
    protected static final EnumSet<Flag> DEFAULT_FLAG = EnumSet.of(Flag.DEFAULT);

    protected static final byte[] NO_DATA = new byte[0];

    // Externalizable do rely on serialVersionUID
    static final long serialVersionUID = 2017_05_29L;

    /** @deprecated 10.2 use {@link #getWatermark()} or {@link #setWatermark(long)} instead */
    @Deprecated
    public long watermark;

    /** @deprecated 10.2 use {@link #getKey()} or {@link #setKey(String)} instead */
    @Deprecated
    public String key;

    /** @deprecated 10.2 use {@link #getData()} or {@link #setData(byte[])} instead */
    @Deprecated
    // We can not use null because Nullable on byte[] requires avro 1.7.6 cf AVRO-1401
    public byte[] data = NO_DATA;

    /** @deprecated 10.2 use {@link #getFlags()} or {@link #setFlags(EnumSet)} instead */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    // The enumSet representation of the flags is transient because serializer don't handle this type
    public transient EnumSet<Flag> flags;

    protected byte flagsAsByte;

    public Record() {
        // Empty constructor required for deserialization
    }

    /**
     * Creates a record using current watermark corresponding to the current time, with a default flag
     */
    public Record(String key, byte[] data) {
        this(key, data, Watermark.ofNow().getValue(), DEFAULT_FLAG);
    }

    /**
     * Creates a record using a default flag
     */
    public Record(String key, byte[] data, long watermark) {
        this(key, data, watermark, DEFAULT_FLAG);
    }

    public Record(String key, byte[] data, long watermark, EnumSet<Flag> flags) {
        this.key = key;
        this.watermark = watermark;
        setData(data);
        setFlags(flags);
    }

    /**
     * Creates a record using current timestamp and default flag
     */
    public static Record of(String key, byte[] data) {
        return new Record(key, data);
    }

    public long getWatermark() {
        return watermark;
    }

    public void setWatermark(long watermark) {
        this.watermark = watermark;
    }

    public EnumSet<Flag> getFlags() {
        if (flags == null) {
            flags = decodeFlags(flagsAsByte);
        }
        return flags;
    }

    public void setFlags(EnumSet<Flag> flags) {
        this.flags = flags;
        this.flagsAsByte = (byte) encodeFlags(flags);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        if (data != null) {
            this.data = data;
        } else {
            this.data = NO_DATA;
        }
    }

    @Override
    public String toString() {
        String wmDate = "";
        if (watermark > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Watermark wm = Watermark.ofValue(watermark);
            wmDate = ", wmDate=" + dateFormat.format(new Date(wm.getTimestamp()));
        }
        return "Record{" + "watermark=" + watermark + wmDate + ", flags=" + getFlags() + ", key='" + key + '\''
                + ", data.length=" + ((data == null) ? 0 : data.length) + ", data=\"" + dataOverview(127) + "\"}";
    }

    public String dataOverview(int maxLength) {
        String overview = "";
        if (data != null && data.length > 0) {
            String dataAsString = new String(data, StandardCharsets.UTF_8);
            overview = dataAsString.substring(0, min(dataAsString.length(), maxLength));
            overview = overview.replaceAll("[^\\x20-\\x7e]", ".");
        }
        return overview;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(watermark);
        // use a short for backward compatibility
        out.writeShort(flagsAsByte);
        out.writeObject(key);
        if (data == null || data.length == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(data.length);
            out.write(data);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.watermark = in.readLong();
        // use a short for backward compatibility
        this.flagsAsByte = (byte) in.readShort();
        this.key = (String) in.readObject();
        int dataLength = in.readInt();
        if (dataLength == 0) {
            this.data = NO_DATA;
        } else {
            this.data = new byte[dataLength];
            // not using in.readFully because it is not impl by Chronicle WireObjectInput
            int pos = 0;
            while (pos < dataLength) {
                int byteRead = in.read(this.data, pos, dataLength - pos);
                if (byteRead == -1) {
                    throw new IllegalStateException("Corrupted stream, can not read " + dataLength + " bytes");
                }
                pos += byteRead;
            }
        }
    }

    protected short encodeFlags(EnumSet<Flag> enumSet) {
        // adapted from Adamski: http://stackoverflow.com/questions/2199399/storing-enumset-in-a-database
        short ret = 0;
        if (enumSet != null) {
            for (Flag val : enumSet) {
                ret = (short) (ret | (1 << val.ordinal()));
            }
        }
        return ret;
    }

    protected EnumSet<Flag> decodeFlags(byte encoded) {
        // adapted from Adamski: http://stackoverflow.com/questions/2199399/storing-enumset-in-a-database
        Map<Integer, Flag> ordinalMap = new HashMap<>();
        for (Flag val : Flag.ALL_OPTS) {
            ordinalMap.put(val.ordinal(), val);
        }
        EnumSet<Flag> ret = EnumSet.noneOf(Flag.class);
        int ordinal = 0;
        for (byte i = 1; i != 0; i <<= 1) {
            if ((i & encoded) != 0) {
                ret.add(ordinalMap.get(ordinal));
            }
            ++ordinal;
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Record record = (Record) o;
        return watermark == record.watermark && flagsAsByte == record.flagsAsByte && Objects.equals(key, record.key)
                && Arrays.equals(data, record.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(watermark, flagsAsByte, key);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public enum Flag {
        // limited to 8 flags so it can be encoded as a byte
        DEFAULT, COMMIT, POISON_PILL, SKIP, TRACE, PAUSE, USER1, USER2;

        public static final EnumSet<Flag> ALL_OPTS = EnumSet.allOf(Flag.class);
    }
}
