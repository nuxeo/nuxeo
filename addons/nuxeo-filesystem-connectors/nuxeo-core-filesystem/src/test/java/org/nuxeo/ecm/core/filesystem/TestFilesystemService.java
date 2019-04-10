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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.filesystem;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.filesystem.FilesystemService;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.filesystem.FilesystemServiceImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestFilesystemService extends SQLRepositoryTestCase {

    protected FilesystemService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.filesystem");
        service = Framework.getService(FilesystemService.class);
        assertNotNull(service);
    }

    public void testClean() throws Exception {
        FilesystemServiceImpl s = new FilesystemServiceImpl();
        assertNull(s.clean("-"));
        assertNull(s.clean("/\\-,?"));
        assertEquals("foo", s.clean("foo"));
        assertEquals("foo", s.clean("  foo "));
        assertEquals("foo.", s.clean("foo."));
        assertEquals("foo.txt", s.clean("foo.txt"));
        assertEquals("foo-bar", s.clean("foo/bar"));
    }

    @SuppressWarnings("static-access")
    public void testGetSuffix() {
        FilesystemServiceImpl s = new FilesystemServiceImpl();
        assertEquals(".c", s.getSuffix("foo.c"));
        assertEquals(".cc", s.getSuffix("foo.cc"));
        assertEquals(".bar", s.getSuffix("foo.bar"));
        assertEquals(".mp3", s.getSuffix("foo.mp3"));
        assertEquals(".jpeg", s.getSuffix("foo.jpeg"));
        assertEquals(".abcdef", s.getSuffix("foo.abcdef"));
        assertEquals(".baz", s.getSuffix("foo.bar.baz"));
        assertEquals(".bar", s.getSuffix("foo.bar  "));
        assertEquals("", s.getSuffix(""));
        assertEquals("", s.getSuffix("foo"));
        assertEquals("", s.getSuffix("foo."));
        assertEquals("", s.getSuffix("foo.abcdefghijkl"));
        assertEquals("", s.getSuffix("foo.a b"));
    }

    public void testSet() throws Exception {
        assertNames("foo", newDoc(), "foo", null, null, null);
        assertNames("foo.txt", newDoc(), "foo.txt", null, null, null);
        assertNames("mytitle.bin", newDoc(), null, "mytitle", null, null);
        assertNames("mytitle.bin", newDoc(), null, "mytitle.txt", null, null);
        assertNames("mytitle.pdf", newDoc(), null, "mytitle", null, "bar.pdf");
        assertNames("mytitle.pdf", newDoc(), null, "mytitle.txt", null,
                "bar.pdf");
        assertNames("myname.bin", newDoc(), null, null, "myname", null);
        assertNames("myname.bin", newDoc(), null, null, "myname.txt", null);
        assertNames("myname.pdf", newDoc(), null, null, "myname", "bar.pdf");
        assertNames("myname.pdf", newDoc(), null, null, "myname.txt", "bar.pdf");
    }

    protected DocumentModelImpl newDoc() throws PropertyException,
            ClientException {
        DocumentModelImpl doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("dc:title", "oldtitle");
        StringBlob blob = new StringBlob("blob", "text/plain", "UTF-8");
        blob.setFilename("oldblob.bin");
        doc.setPropertyValue("file:content", blob);
        doc.setPathInfo("/foo", "path");
        return doc;
    }

    protected void assertNames(String expected, DocumentModel doc,
            String filename, String title, String name, String oldFilename)
            throws Exception {
        service.set(doc, filename, title, name, oldFilename);
        assertEquals("Bad filename", expected, service.getFilename(doc));
        assertEquals("Bad title", expected, service.getTitle(doc));
        assertEquals("Bad name", expected, service.getName(doc));
    }

}
