/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.JSONBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class consists exclusively of static methods that operate on {@link Blob}s.
 */
public class Blobs {

    private Blobs() {
    }

    /**
     * Creates a {@link Blob} backed by the given {@link File}.
     *
     * @param file file
     */
    public static Blob createBlob(File file) throws IOException {
        return new FileBlob(file, null, null);
    }

    /**
     * Creates a {@link Blob} backed by the given {@link File}.
     *
     * @param file file
     * @param mimeType the MIME type
     */
    public static Blob createBlob(File file, String mimeType) throws IOException {
        return new FileBlob(file, mimeType, null);
    }

    /**
     * Creates a {@link Blob} backed by the given {@link File}.
     *
     * @param file file
     * @param mimeType the MIME type
     * @param encoding the encoding
     */
    public static Blob createBlob(File file, String mimeType, String encoding) throws IOException {
        return new FileBlob(file, mimeType, encoding);
    }

    /**
     * Creates a {@link Blob} backed by the given {@link File}.
     *
     * @param file file
     * @param mimeType the MIME type
     * @param encoding the encoding
     * @param filename the blob filename
     */
    public static Blob createBlob(File file, String mimeType, String encoding, String filename) throws IOException {
        return new FileBlob(file, mimeType, encoding, filename, null);
    }

    /**
     * Creates a {@link Blob} backed by an empty temporary {@link File} with the given extension.
     *
     * @param ext the extension
     */
    public static Blob createBlobWithExtension(String ext) throws IOException {
        return new FileBlob(ext);
    }

    /**
     * Creates a {@link Blob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     */
    public static Blob createBlob(InputStream in) throws IOException {
        return new FileBlob(in, null, null, null);
    }

    /**
     * Creates a {@link Blob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     */
    public static Blob createBlob(InputStream in, String mimeType) throws IOException {
        return new FileBlob(in, mimeType, null, null);
    }

    /**
     * Creates a {@link Blob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     * @param encoding the encoding
     */
    public static Blob createBlob(InputStream in, String mimeType, String encoding) throws IOException {
        return new FileBlob(in, mimeType, encoding, null);
    }

    /**
     * Creates a {@link Blob} from an {@link InputStream}, by saving it to a temporary file.
     * <p>
     * The input stream is closed.
     *
     * @param in the input stream, which is closed after use
     * @param mimeType the MIME type
     * @param encoding the encoding
     * @param tmpDir the temporary directory for file creation
     */
    public static Blob createBlob(InputStream in, String mimeType, String encoding, File tmpDir) throws IOException {
        return new FileBlob(in, mimeType, encoding, tmpDir);
    }

    /**
     * Creates a {@link Blob} backed by the given bytes array.
     *
     * @param bytes the bytes array
     */
    public static Blob createBlob(byte[] bytes) {
        return new ByteArrayBlob(bytes, null, null);
    }

    /**
     * Creates a {@link Blob} backed by the given bytes array.
     *
     * @param bytes the bytes array
     * @param mimeType the MIME type
     */
    public static Blob createBlob(byte[] bytes, String mimeType) throws IOException {
        return new ByteArrayBlob(bytes, mimeType, null);
    }

    /**
     * Creates a {@link Blob} backed by the given bytes array.
     *
     * @param bytes the bytes array
     * @param mimeType the MIME type
     * @param encoding the encoding
     */
    public static Blob createBlob(byte[] bytes, String mimeType, String encoding) throws IOException {
        return new ByteArrayBlob(bytes, mimeType, encoding);
    }

    /**
     * Creates a {@link Blob} backed by the given string, with MIME type "text/plain" and encoding "UTF-8".
     *
     * @param string the string
     */
    public static Blob createBlob(String string) {
        return new StringBlob(string); // "text/plain", "UTF-8"
    }

    /**
     * Creates a {@link Blob} backed by the given string, with encoding "UTF-8".
     *
     * @param string the string
     * @param mimeType the MIME type
     */
    public static Blob createBlob(String string, String mimeType) {
        return new StringBlob(string, mimeType); // "UTF-8"
    }

    /**
     * Creates a {@link Blob} backed by the given string.
     *
     * @param string the string
     * @param mimeType the MIME type
     * @param encoding the encoding
     */
    public static Blob createBlob(String string, String mimeType, String encoding) {
        return new StringBlob(string, mimeType, encoding);
    }

    /**
     * Creates a {@link Blob} backed by the given string.
     *
     * @param string the string
     * @param mimeType the MIME type
     * @param encoding the encoding
     * @param filename the blob filename
     */
    public static Blob createBlob(String string, String mimeType, String encoding, String filename) {
        return new StringBlob(string, mimeType, encoding, filename);
    }

    /**
     * Create a {@link Blob} backed by the given JSON string.
     *
     * @param json the JSON string
     * @since 9.2
     */
    public static Blob createJSONBlob(String json) {
        return new JSONBlob(json);
    }

    /**
     * Create a {@link Blob} backed by the JSON for an arbitrary value.
     * <p>
     * The value's internal classes may be annotated with Jackson 2 annotations.
     *
     * @param value the value
     * @since 9.2
     */
    public static Blob createJSONBlobFromValue(Object value) throws IOException {
        String json = new ObjectMapper().writeValueAsString(value);
        return new JSONBlob(json);
    }

}
