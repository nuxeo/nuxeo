/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.importer.stream.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.nuxeo.lib.stream.pattern.Message;

/**
 * A message holding info to build a StringBlob.
 *
 * @since 9.1
 */
public class BlobMessage implements Message {
    static final long serialVersionUID = 20170529L;

    protected String mimetype;

    protected String encoding;

    protected String filename;

    protected String path;

    protected String content;

    public BlobMessage() {
    }

    protected BlobMessage(StringMessageBuilder builder) {
        mimetype = builder.mimetype;
        encoding = builder.encoding;
        filename = builder.filename;
        path = builder.path;
        content = builder.content;
        if ((path == null || path.isEmpty()) && ((content == null) || content.isEmpty())) {
            throw new IllegalArgumentException("BlobMessage must be initialized with a file path or content");
        }
    }

    @Override
    public String getId() {
        return filename;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    public String getPath() {
        return path;
    }

    public String getEncoding() {
        return encoding;
    }

    public static class StringMessageBuilder {
        protected String mimetype;

        protected String encoding;

        protected String filename;

        protected String path;

        protected String content;

        /**
         * Create a string blob with a content
         */
        public StringMessageBuilder(String content) {
            this.content = content;
        }

        /**
         * Set the name of the file.
         */
        public StringMessageBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Set the path of the file containing the blob content.
         */
        public StringMessageBuilder setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Set the mime-type of the file.
         */
        public StringMessageBuilder setMimetype(String mimetype) {
            this.mimetype = mimetype;
            return this;
        }

        protected StringMessageBuilder setPath(String path) {
            this.path = path;
            // either a path or a content not both
            this.content = null;
            return this;
        }

        public BlobMessage build() {
            return new BlobMessage(this);
        }
    }

    public static class FileMessageBuilder extends StringMessageBuilder {
        /**
         * Create a blob from a file
         */
        public FileMessageBuilder(String path) {
            super(null);
            this.setPath(path);
            this.filename = Paths.get(path).getFileName().toString();
            this.mimetype = guessMimeType();
        }

        protected String guessMimeType() {
            try {
                return Files.probeContentType(Paths.get(path));
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(mimetype);
        out.writeObject(encoding);
        out.writeObject(filename);
        out.writeObject(path);
        out.writeObject(content);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        mimetype = (String) in.readObject();
        encoding = (String) in.readObject();
        filename = (String) in.readObject();
        path = (String) in.readObject();
        content = (String) in.readObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BlobMessage that = (BlobMessage) o;

        if (mimetype != null ? !mimetype.equals(that.mimetype) : that.mimetype != null)
            return false;
        if (encoding != null ? !encoding.equals(that.encoding) : that.encoding != null)
            return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null)
            return false;
        if (path != null ? !path.equals(that.path) : that.path != null)
            return false;
        return content != null ? content.equals(that.content) : that.content == null;
    }

    @Override
    public int hashCode() {
        int result = mimetype != null ? mimetype.hashCode() : 0;
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BlobMessage{" + "mimetype='" + mimetype + '\'' + ", encoding='" + encoding + '\'' + ", filename='"
                + filename + '\'' + ", path='" + path + '\'' + ", content='" + content + '\'' + '}';
    }
}
