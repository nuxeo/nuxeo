/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.automation.server.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.StreamBlob;
import org.nuxeo.ecm.platform.web.common.ServletHelper;

/**
 * @author matic
 */
public class UploadFileSupport {

    protected final String rootPath;

    protected final Session session;

    public UploadFileSupport(Session session, String rootPath) {
        this.session = session;
        this.rootPath = rootPath;
    }

    public static class MockInputStream extends InputStream {

        final long max;

        long consumed = 0;

        public MockInputStream(long size) {
            max = size;
        }

        @Override
        public int read() throws IOException {
            if (consumed >= max) {
                return -1;
            }
            consumed += 1;
            return getByte();
        }

        int getByte() {
            return 0;
        }
    }

    public static class DigestMockInputStream extends MockInputStream {

        protected final Random rand = new Random();

        protected final MessageDigest digest = MessageDigest.getInstance("MD5");

        DigestMockInputStream(long size) throws NoSuchAlgorithmException {
            super(size);
        }

        @Override
        int getByte() {
            byte data = (byte) rand.nextInt(255);
            digest.update(data);
            return data;
        }

    }

    public static MockInputStream newMockInput(long size, boolean digest) throws NoSuchAlgorithmException {
        if (digest) {
            return new DigestMockInputStream(size);
        }
        return new MockInputStream(size);
    }

    public FileInputStream testUploadFile(InputStream source) throws Exception {
        return testUploadFile(source, 0);
    }

    public FileInputStream testUploadFile(InputStream source, int timeout) throws Exception {
        Blob blob = new StreamBlob(source, "big-blob", "application/octet-stream");
        Document root = (Document) session.newRequest("Document.Fetch").set("value", rootPath).execute();
        OperationRequest upload = session.newRequest("Document.Create").setInput(root).set("type", "File").set("name",
                "bigfile").set("properties", "dc:title=Big File");
        if (timeout > 0) {
            upload = upload.setHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY, Integer.toString(timeout));
        }
        Document doc = (Document) upload.execute();
        session.newRequest("Blob.Attach").setHeader(Constants.HEADER_NX_VOIDOP, "true").setInput(blob).set("document",
                rootPath + "/bigfile").execute();
        FileBlob serverBlob = (FileBlob) session.newRequest("Blob.Get").setInput(doc).set("xpath", "file:content").execute();
        return (FileInputStream) serverBlob.getStream();
    }

}
