/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class UploadFileSupport {

    public static class MockInputStream extends InputStream {

        long consumed = 0;

        final long max;

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

        DigestMockInputStream(long size) throws NoSuchAlgorithmException {
            super(size);
        }

        protected final Random rand = new Random();

        protected final MessageDigest digest = MessageDigest.getInstance("MD5");

        @Override
        int getByte() {
            byte data = (byte) rand.nextInt(255);
            digest.update(data);
            return data;
        }

        boolean checkDigest(InputStream in) throws IOException {
            byte[] sentSum = digest.digest();
            while (in.available() > 0) {
                digest.update((byte) in.read());
            }
            byte[] receivedSum = digest.digest();
            return MessageDigest.isEqual(sentSum, receivedSum);
        }
    }

   public static MockInputStream newMockInput(long size, boolean digest) throws NoSuchAlgorithmException {
       if (digest) {
           return new DigestMockInputStream(size);
       }
       return new MockInputStream(size);
   }

    public UploadFileSupport(Session session) {
        this.session = session;
    }

    protected final Session session;

    public FileInputStream testUploadFile(InputStream source) throws Exception {
        return testUploadFile(source, 0);
    }

    public FileInputStream testUploadFile(InputStream source, int timeout) throws Exception {
        Blob blob = new StreamBlob(source, "big-blob", "application/octet-stream");
        Document root = (Document) session.newRequest("Document.Fetch").set(
                "value", "/").execute();
        OperationRequest upload = session.newRequest("Document.Create").setInput(
                root).set("type", "File").set("name", "bigfile").set(
                "properties", "dc:title=Big File");
        if (timeout > 0) {
            upload = upload.setHeader(ServletHelper.TX_TIMEOUT_HEADER_KEY, Integer.toString(timeout));
        }
        Document doc = (Document)upload.execute();
        session.newRequest("Blob.Attach").setHeader(Constants.HEADER_NX_VOIDOP,
                "true").setInput(blob).set("document", "/bigfile").execute();
        FileBlob serverBlob = (FileBlob) session.newRequest("Blob.Get").setInput(
                doc).set("xpath", "file:content").execute();
        return (FileInputStream)serverBlob.getStream();
    }

}
