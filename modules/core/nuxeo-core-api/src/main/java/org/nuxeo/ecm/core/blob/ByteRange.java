/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * A byte range.
 *
 * @since 11.1
 */
public class ByteRange {

    protected final long start;

    /** The end is inclusive. */
    protected final long end;

    protected ByteRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Constructs a byte range from a start and INCLUSIVE end.
     *
     * @param start the start
     * @param end the INCLUSIVE end
     */
    public static ByteRange inclusive(long start, long end) {
        return new ByteRange(start, end);
    }

    /** The start. */
    public long getStart() {
        return start;
    }

    /** The end, which is INCLUSIVE. */
    public long getEnd() {
        return end;
    }

    /** The length. */
    public long getLength() {
        return end - start + 1;
    }

    /**
     * Returns a sub-stream of the given stream to which this byte range has been applied.
     *
     * @param stream the stream
     * @return a sub-stream for this byte range
     */
    public InputStream forStream(InputStream stream) throws IOException {
        try {
            // avoid using IOUtils.skipFully, which uses read() under the hood
            long remain = getStart();
            while (remain > 0) {
                long n = stream.skip(remain);
                if (n < 0) {
                    throw new EOFException();
                }
                if (n == 0) {
                    throw new IOException("Failed to skip in stream");
                }
                remain -= n;
            }
        } catch (IOException e) {
            stream.close();
            throw e;
        }
        return new BoundedInputStream(stream, getLength());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + start + '-' + end + ')';
    }

}
