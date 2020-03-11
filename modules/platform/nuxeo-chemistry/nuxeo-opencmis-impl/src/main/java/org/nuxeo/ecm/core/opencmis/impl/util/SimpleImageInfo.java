/*
 * This code is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Source
 *   http://blog.jaimon.co.uk/simpleimageinfo/SimpleImageInfo.java.html
 *   http://jaimonmathew.wordpress.com/2011/01/29/simpleimageinfo/
 * Revision history
 *   0.1 - 29/Jan/2011 - Initial version created
 *   0.x - 25/Oct/2011 - Refactoring and cleanups, adding length (Nuxeo)
 *
 * @author Jaimon Mathew <http://www.jaimon.co.uk>
 * @author Florent Guillaume <http://nuxeo.com/>
 */

package org.nuxeo.ecm.core.opencmis.impl.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Java class to determine image width, height, length and MIME types for a number of image file formats without
 * loading the whole image data.
 */
public class SimpleImageInfo {

    protected int width;

    protected int height;

    protected long length;

    protected String mimeType;

    protected InputStream in;

    public SimpleImageInfo(InputStream stream) throws IOException {
        if (stream instanceof BufferedInputStream) {
            in = stream;
        } else {
            in = new BufferedInputStream(stream);
        }
        processStream();
        finishStream();
        stream.close();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getLength() {
        return length;
    }

    public String getMimeType() {
        return mimeType;
    }

    protected void processStream() throws IOException {
        int c1 = read();
        int c2 = read();
        int c3 = read();

        mimeType = "application/octet-stream";
        width = -1;
        height = -1;

        if (c1 == 'G' && c2 == 'I' && c3 == 'F') { // GIF
            skip(3);
            width = readInt(2, false);
            height = readInt(2, false);
            mimeType = "image/gif";
        } else if (c1 == 0xFF && c2 == 0xD8) { // JPG
            while (c3 == 255) {
                int marker = read();
                int len = readInt(2, true);
                if (marker == 192 || marker == 193 || marker == 194) {
                    skip(1);
                    height = readInt(2, true);
                    width = readInt(2, true);
                    mimeType = "image/jpeg";
                    break;
                }
                skip(len - 2);
                c3 = read();
            }
        } else if (c1 == 137 && c2 == 80 && c3 == 78) { // PNG
            skip(15);
            width = readInt(2, true);
            skip(2);
            height = readInt(2, true);
            mimeType = "image/png";
        } else if (c1 == 66 && c2 == 77) { // BMP
            skip(15);
            width = readInt(2, false);
            skip(2);
            height = readInt(2, false);
            mimeType = "image/bmp";
        } else {
            int c4 = read();
            if ((c1 == 'M' && c2 == 'M' && c3 == 0 && c4 == 42) || (c1 == 'I' && c2 == 'I' && c3 == 42 && c4 == 0)) { // TIFF
                boolean bigEndian = c1 == 'M';
                int ifd = 0;
                int entries;
                ifd = readInt(4, bigEndian);
                skip(ifd - 8);
                entries = readInt(2, bigEndian);
                for (int i = 1; i <= entries; i++) {
                    int tag = readInt(2, bigEndian);
                    int fieldType = readInt(2, bigEndian);
                    readInt(4, bigEndian); // count unused
                    int valOffset;
                    if ((fieldType == 3 || fieldType == 8)) {
                        valOffset = readInt(2, bigEndian);
                        skip(2);
                    } else {
                        valOffset = readInt(4, bigEndian);
                    }
                    if (tag == 256) {
                        width = valOffset;
                    } else if (tag == 257) {
                        height = valOffset;
                    }
                    if (width != -1 && height != -1) {
                        mimeType = "image/tiff";
                        break;
                    }
                }
            }
        }
    }

    protected int read() throws IOException {
        int c = in.read();
        if (c != -1) {
            length++;
        }
        return c;
    }

    protected void skip(int n) throws IOException {
        long done = 0;
        while (done < n) {
            long num = in.skip(n - done);
            if (num == 0) {
                break;
            }
            done += num;
        }
        length += done;
    }

    protected void finishStream() throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            length += n;
        }
    }

    protected int readInt(int noOfBytes, boolean bigEndian) throws IOException {
        int ret = 0;
        int sv = bigEndian ? (noOfBytes - 1) * 8 : 0;
        int cnt = bigEndian ? -8 : 8;
        for (int i = 0; i < noOfBytes; i++) {
            ret |= read() << sv;
            sv += cnt;
        }
        return ret;
    }

}
