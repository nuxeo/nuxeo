/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestMime {

    public static void main(String[] args) throws Exception {
        final MimeMultipart mp = new MimeMultipart("related");

        MimeBodyPart part = new MimeBodyPart();
        part.setText("bla bla", "UTF-8");
        part.setContentID("request");
        part.setHeader("Content-Type", Constants.CTYPE_REQUEST);
        part.setHeader("Content-Transfer-Encoding", "8bit");
        part.setHeader("Content-Length", "5");
        mp.addBodyPart(part);

        part = new MimeBodyPart();
        part.attachFile(new File("/Users/bstefanescu/anonymous-user-config.xml"));
        part.setHeader("Content-Type", "application/octet-stream");
        part.setHeader("Content-Transfer-Encoding", "binary");
        part.setContentID("input");

        mp.addBodyPart(part);

        part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new DataSource() {

            protected ByteArrayInputStream in = new ByteArrayInputStream("abcdefgh".getBytes());

            public OutputStream getOutputStream() throws IOException {
                throw new UnsupportedOperationException("not writeable");
            }

            public String getName() {
                return "my in stream";
            }

            public InputStream getInputStream() throws IOException {
                return in;
            }

            public String getContentType() {
                return "application/octet-stream";
            }
        }));

        part.setHeader("Content-Type", "application/octet-stream");
        part.setHeader("Content-Transfer-Encoding", "binary");
        part.setContentID("is");

        mp.addBodyPart(part);

        System.out.println(mp.getContentType());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mp.writeTo(out);
        out.writeTo(System.out);

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        DataSource ds = new DataSource() {

            public OutputStream getOutputStream() throws IOException {
                throw new UnsupportedOperationException("not writeable");
            }

            public String getName() {
                return "bla";
            }

            public InputStream getInputStream() throws IOException {
                return in;
            }

            public String getContentType() {
                return mp.getContentType();
            }
        };
        MimeMultipart body = new MimeMultipart(ds);

        System.out.println(body.getContentType());
        System.out.println(body.getBodyPart("request"));
        System.out.println(body.getBodyPart("input"));

        DataHandler dh = body.getBodyPart("request").getDataHandler();

        System.out.println(FileUtils.read(dh.getInputStream()));


    }

}
