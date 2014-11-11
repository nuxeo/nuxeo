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

public class TestRemoteFileManagerService extends TestFake {

    private FileManagerService filemanagerService;
    private static final String filePath = "test-data/hello.doc";

    @Override
    public void setUp() throws Exception {
        super.setUp();
//        CoreSession docMag = (CoreSession) new InitialContext()
//                .lookup(JNDILocationsLoader.documentManagerLocation);
        filemanagerService = new FileManagerService();
    }

    @Override
    public void tearDown() throws Exception {
        filemanagerService = null;
        super.tearDown();
    }

    public void testCreateFromBlob() throws Exception {
        DocumentModel root = remote.getRootDocument();
        URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
        File file = new File(url.toURI());

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/pdf");

        DocumentModel doc = filemanagerService.defaultCreate(
                remote, input, root.getPathAsString(), true, url.toString());
        assertNotNull(doc);
    }

    /*
     * public void testCreateDocumentFromFiles() throws Exception{
     *
     * List<File> files= new ArrayList<File>(); for(Object o :
     * properties.keySet()){ String key = (String) o; if(key!=null &&
     * key.startsWith("test.crea.file")){ String filePath =
     * properties.getProperty(key); File file = new File(filePath);
     * files.add(file); } }
     *
     * DocumentModel root = remote.getRootDocument(); List<DocumentModel>
     * docList =
     * filemanagerService.createDocumentFromFiles(files,root.getPathAsString(),
     * true); }
     */
    public void testUpdateDocumentFromBlob() throws Exception {
        DocumentModel root = remote.getRootDocument();

        URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
        File file = new File(url.toURI());

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/pdf");

        DocumentModel updateDoc = filemanagerService.updateDocumentFromBlob(
                remote, input, root.getPathAsString(), filePath);

        // TODO: make this work below.

        //assertNotNull(updateDoc);
        //updateDoc = filemanagerService.updateDocumentFromBlob(
        //        remote, input, root.getPathAsString(), filePath);
        //assertNotNull(updateDoc);
    }

    /*
     *
     * public void testUpdateDocumentFromFiles() throws Exception{
     *
     * Map<String, File> pathFiles= new HashMap<String, File>(); DocumentModel
     * root = remote.getRootDocument(); for(Object o : properties.keySet()){
     * String key = (String) o; if(key!=null &&
     * key.startsWith("test.upda.file")){ String filePath =
     * properties.getProperty(key); File file = new File(filePath);
     * pathFiles.put(root.getPathAsString(), file); } }
     *
     *
     * List<DocumentModel> docList =
     * filemanagerService.updateDocumentFromFiles(pathFiles); }
     *
     * public void testCreateDocumentFromFolder() throws Exception{ String
     * filePath = properties.getProperty("test.folder"); DocumentModel root =
     * remote.getRootDocument();
     *
     * File file = new File(filePath);
     *
     * DocumentModel doc =
     * filemanagerService.createDocumentFromFile(file,root.getPathAsString(),
     * true,null); assertNotNull(doc); }
     */

}
