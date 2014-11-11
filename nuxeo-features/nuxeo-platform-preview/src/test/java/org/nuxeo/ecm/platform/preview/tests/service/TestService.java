/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.preview.tests.service;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.preview.adapter.MimeTypePreviewer;
import org.nuxeo.ecm.platform.preview.adapter.PreviewAdapterManager;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Test Preview adapter management service registration
 *
 * @author tiry
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.convert",
        "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.preview",
        "org.nuxeo.ecm.platform.dublincore"})
public class TestService {

    @Test
    public void testService() {
        PreviewAdapterManager pam = Framework.getLocalService(PreviewAdapterManager.class);
        assertNotNull(pam);
        MimeTypePreviewer previewer = pam.getPreviewer("text/html");
        assertNotNull(previewer);
    }

    @Test
    // disabled because failing under windows, see NXP-12666
    @Ignore
    public void testLatin1() throws IOException, ClientException, MimetypeNotFoundException, MimetypeDetectionException {
        DocumentModel doc = repo.createDocumentModel("/", "fake", "File");
        doc = repo.createDocument(doc);
        checkLatin1(doc, "latin1.txt", "text/plain");
        checkLatin1(doc, "latin1.xhtml", "text/html");
        checkLatin1(doc, "latin1.csv", "text/csv");
    }

    @Inject
    protected CoreSession repo;

    protected final Pattern charsetPattern = Pattern.compile("content=\".*;\\s*charset=(.*)\"");

    public void checkLatin1(DocumentModel doc, String name, String mtype) throws IOException, ClientException, MimetypeNotFoundException, MimetypeDetectionException {
        File file = new File(getClass().getResource("/" + name).getPath());
        Blob blob = new FileBlob(file);
        blob.setMimeType(mtype);
        doc.getAdapter(BlobHolder.class).setBlob(blob);
        HtmlPreviewAdapter adapter = doc.getAdapter(HtmlPreviewAdapter.class);
        Blob htmlBlob = adapter.getFilePreviewBlobs().get(0);
        String htmlContent = htmlBlob.getString().toLowerCase();
        Matcher matcher = charsetPattern.matcher(htmlContent);
        Assert.assertThat(matcher.find(), Matchers.is(true));
        String charset = matcher.group();
        if ("windows-1252".equals(charset)) {
            Assert.assertThat(htmlContent, Matchers.containsString("test de pr&Atilde;&copy;visualisation avant rattachement"));
        } else {
            Assert.assertThat(htmlContent, Matchers.containsString("test de prévisualisation avant rattachement"));
        }
     }
}
