/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     annejubert
 */

package org.nuxeo.io.fsexporter.test;

import java.io.File;
import java.io.Serializable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 *
 * @author annejubert
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-fsexporter" })
public class FSExporterTestStructure {

    @Inject
    CoreSession session;

    @Inject
    FSExporter service;

    @Test
    public void shouldExportFile() throws Exception {

        // creation of subfolders in sections, templates and workspaces
        DocumentModel mySection = session.createDocumentModel(
                "/default-domain/sections", "mySection", "Folder");
        mySection.setPropertyValue("dc:title", "My first section");
        session.createDocument(mySection);

        DocumentModel myWorkspace = session.createDocumentModel(
                "/default-domain/workspaces", "myWorkspace", "Folder");
        myWorkspace.setPropertyValue("dc:title", "my first workspace");
        session.createDocument(myWorkspace);

        DocumentModel myTemplate = session.createDocumentModel(
                "/default-domain/templates", "myTemplate", "Folder");
        myTemplate.setPropertyValue("dc:title", "my first template");
        session.createDocument(myTemplate);

        // creation of files in the new subfolders
        DocumentModel fileInSection = session.createDocumentModel(
                mySection.getPathAsString(), "fileInSection", "File");
        fileInSection.setPropertyValue("dc:title", "my file in section");

        Blob blobSection = new StringBlob("some content");
        blobSection.setFilename("My File In Section.txt");
        blobSection.setMimeType("text/plain");
        fileInSection.setPropertyValue("file:content",
                (Serializable) blobSection);
        session.createDocument(fileInSection);

        DocumentModel fileInWorkspace = session.createDocumentModel(
                myWorkspace.getPathAsString(), "fileInWorkspace", "File");
        fileInWorkspace.setPropertyValue("dc:title", "my file in workspace");

        Blob blobWorkspace = new StringBlob("some content");
        blobWorkspace.setFilename("My File In Workspace.txt");
        blobWorkspace.setMimeType("text/plain");
        fileInWorkspace.setPropertyValue("file:content",
                (Serializable) blobWorkspace);
        session.createDocument(fileInWorkspace);

        DocumentModel fileInTemplate = session.createDocumentModel(
                myTemplate.getPathAsString(), "fileInTemplate", "File");
        fileInTemplate.setPropertyValue("dc:title", "my file in template");

        Blob blobTemplate = new StringBlob("some content");
        blobTemplate.setFilename("My File In Template.txt");
        blobTemplate.setMimeType("text/plain");
        fileInTemplate.setPropertyValue("file:content",
                (Serializable) blobTemplate);
        session.createDocument(fileInTemplate);

        session.save();

        Framework.getLocalService(FSExporter.class);
        service.export(session, "/default-domain/", "/tmp/", "GET_CHILDREN_PP");

        // verify that My File In Section.txt exists
        String targetPathSection = "/tmp" + mySection.getPathAsString() + "/"
                + blobSection.getFilename();
        Assert.assertTrue(new File(targetPathSection).exists());

        // verify that My File In Workspace.txt exists
        String targetPathWorkspace = "/tmp" + myWorkspace.getPathAsString()
                + "/" + blobWorkspace.getFilename();
        Assert.assertTrue(new File(targetPathWorkspace).exists());

        // verify that My File In Template.txt exists
        String targetPathTemplate = "/tmp" + myTemplate.getPathAsString() + "/"
                + blobTemplate.getFilename();
        Assert.assertTrue(new File(targetPathTemplate).exists());

    }
}
