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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import java.io.File;
import java.net.URL;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;

public class TestFileManagerService extends TestFake {

    private static final String FILE_PATH = "test-data/hello.doc";

    private FileManagerService filemanagerService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        filemanagerService = new FileManagerService();
    }

    @Override
    public void tearDown() throws Exception {
        filemanagerService = null;
        super.tearDown();
    }

    public void testCreateFromBlob() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(FILE_PATH);
        File file = new File(url.toURI());

        DocumentModel root = remote.getRootDocument();

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/pdf");

        DocumentModel doc = filemanagerService.createDocumentFromBlob(
                remote, input, root.getPathAsString(), true, FILE_PATH);
        assertNotNull(doc);
    }

/*
    public void testCreateDocumentFromFiles() throws Exception {

        List<File> files = new ArrayList<File>();
        for (Object o :
                properties.keySet()) {
            String key = (String) o;
            if (key != null &&
                    key.startsWith("test.crea.file")) {
                String filePath =
                        properties.getProperty(key);
                File file = new File(filePath);
                files.add(file);
            }
        }

        DocumentModel root = remote.getRootDocument();
        List<DocumentModel>
                docList =
                filemanagerService.createDocumentFromFiles(remote, files, root.getPathAsString(),
                        true);
    }
*/

    public void testUpdateDocumentFromBlob() throws Exception {
        DocumentModel root = remote.getRootDocument();

        URL url = Thread.currentThread().getContextClassLoader().getResource(FILE_PATH);
        File file = new File(url.toURI());

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/msword");

        DocumentModel updateDoc = filemanagerService.updateDocumentFromBlob(remote, input,
                        root.getPathAsString(), FILE_PATH);
        // TODO: fix this
        // assertNotNull(updateDoc);
        //updateDoc = filemanagerService.updateDocumentFromBlob(remote, input,
        //                root.getPathAsString(), filePath);
        //assertNotNull(updateDoc);
    }


/*
    public void testUpdateDocumentFromFiles() throws Exception {

        Map<String, File> pathFiles = new HashMap<String, File>();
        DocumentModel
                root = remote.getRootDocument();
        for (Object o : properties.keySet()) {
            String key = (String) o;
            if (key != null &&
                    key.startsWith("test.upda.file")) {
                String filePath =
                        properties.getProperty(key);
                File file = new File(filePath);
                pathFiles.put(root.getPathAsString(), file);
            }
        }


        List<DocumentModel> docList =
                filemanagerService.updateDocumentFromFiles(remote, pathFiles);
    }
*/


/*    public void testCreateDocumentFromFolder() throws Exception {
        String filePath = properties.getProperty("test.folder");
        DocumentModel root = remote.getRootDocument();

        File file = new File(filePath);

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, null);

        DocumentModel doc = filemanagerService.createDocumentFromBlob(
                remote, input, root.getPathAsString(), true, filePath);
        assertNotNull(doc);
    }
*/

}
