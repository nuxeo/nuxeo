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

import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.lib.stream.pattern.Message;

/**
 * A message holding BlobInfo.
 *
 * @since 9.3
 */
public class BlobInfoMessage extends BlobInfo implements Message {
    static final long serialVersionUID = 20170803L;

    public BlobInfoMessage() {
    }

    public BlobInfoMessage(BlobInfo info) {
        super(info);
    }

    @Override
    public String getId() {
        return digest;
    }

    @Override
    public String toString() {
        return "BlobInfoMessage{" +
                "key='" + key + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", encoding='" + encoding + '\'' +
                ", filename='" + filename + '\'' +
                ", length=" + length +
                ", digest='" + digest + '\'' +
                '}';
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(length);
        out.writeObject(digest);
        out.writeObject(mimeType);
        out.writeObject(encoding);
        out.writeObject(filename);
        out.writeObject(key);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        length = in.readLong();
        digest = (String) in.readObject();
        mimeType = (String) in.readObject();
        encoding = (String) in.readObject();
        filename = (String) in.readObject();
        key = (String) in.readObject();
    }
}
