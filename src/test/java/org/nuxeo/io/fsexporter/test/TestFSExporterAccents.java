/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     annejubert
 *     Yannis JULIENNE
 *     
 */

package org.nuxeo.io.fsexporter.test;

import java.io.File;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author annejubert
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-fsexporter" })
public class TestFSExporterAccents {

    @Inject
    CoreSession session;

    @Inject
    FSExporter service;

    @Test
    public void shouldExportFileTest() throws Exception {
        // creation of subfolders in sections, templates and workspaces
        DocumentModel mySection = session.createDocumentModel("/default-domain/sections", "ma premiere Section - 1",
                "Folder");
        mySection.setPropertyValue("dc:title", "ma premiere section");
        session.createDocument(mySection);

        // creation of two sections with the same name
        DocumentModel mySectionSameName = session.createDocumentModel("/default-domain/sections",
                "my section same name", "Folder");
        mySectionSameName.setPropertyValue("dc:title", "my section same name");
        session.createDocument(mySectionSameName);

        DocumentModel mySectionSameName2 = session.createDocumentModel("/default-domain/sections",
                "my section same name", "Folder");
        mySectionSameName2.setPropertyValue("dc:title", "my section same name");
        session.createDocument(mySectionSameName2);

        // creation of a workspace with an empty name
        // NXP-20502: Trailing spaces are not allowed on all platforms so we only test leading spaces
        DocumentModel myWorkspaceEmptyName = session.createDocumentModel("/default-domain/workspaces", "  '", "Folder");
        myWorkspaceEmptyName.setPropertyValue("dc:title", "my workspace é empty name");
        session.createDocument(myWorkspaceEmptyName);

        // creation of a workspace with a special character
        DocumentModel myWorkspace = session.createDocumentModel("/default-domain/workspaces", "myWorkspace &",
                "Folder");
        myWorkspace.setPropertyValue("dc:title", "my first workspace !");
        session.createDocument(myWorkspace);

        // creation of a template
        DocumentModel myTemplate = session.createDocumentModel("/default-domain/templates", "myTemplate !", "Folder");
        myTemplate.setPropertyValue("dc:title", "my first template");
        session.createDocument(myTemplate);

        // creation of files in the new subfolders
        DocumentModel fileInSection = session.createDocumentModel(mySection.getPathAsString(), "fileInSection", "File");
        fileInSection.setPropertyValue("dc:title", "my file in section");

        Blob blobSection = new StringBlob("some content");
        blobSection.setFilename("mon deuxieme fichier.txt");
        blobSection.setMimeType("text/plain");
        fileInSection.setPropertyValue("file:content", (Serializable) blobSection);
        session.createDocument(fileInSection);

        // file in a workspace with an empty name
        DocumentModel fileInWorkspaceEmptyName = session.createDocumentModel(myWorkspaceEmptyName.getPathAsString(),
                "fileInWorkspaceEmptyName", "File");
        fileInWorkspaceEmptyName.setPropertyValue("dc:title", "my file in section");

        Blob blobWorkspaceEmptyName = new StringBlob("some content");
        blobWorkspaceEmptyName.setFilename("file in a wokspace with empty name.txt");
        blobWorkspaceEmptyName.setMimeType("text/plain");
        fileInWorkspaceEmptyName.setPropertyValue("file:content", (Serializable) blobWorkspaceEmptyName);
        session.createDocument(fileInWorkspaceEmptyName);

        DocumentModel fileInWorkspace = session.createDocumentModel(myWorkspace.getPathAsString(), "fileInWorkspace",
                "File");
        fileInWorkspace.setPropertyValue("dc:title", "my file in workspace");

        Blob blobWorkspace = new StringBlob("some content");
        blobWorkspace.setFilename("My File In Workspace.txt");
        blobWorkspace.setMimeType("text/plain");
        fileInWorkspace.setPropertyValue("file:content", (Serializable) blobWorkspace);
        session.createDocument(fileInWorkspace);

        DocumentModel fileInTemplate = session.createDocumentModel(myTemplate.getPathAsString(), "fileInTemplate",
                "File");
        fileInTemplate.setPropertyValue("dc:title", "my file in template");

        Blob blobTemplate = new StringBlob("some content");
        blobTemplate.setFilename("My File In Template.txt");
        blobTemplate.setMimeType("text/plain");
        fileInTemplate.setPropertyValue("file:content", (Serializable) blobTemplate);
        session.createDocument(fileInTemplate);

        session.save();

        String tmp = Environment.getDefault().getTemp().getPath();
        service.export(session, "/default-domain/", tmp, "");

        String pathPrefix = StringUtils.removeEnd(tmp, "/");

        String targetPathSection = pathPrefix + mySection.getPathAsString() + "/" + blobSection.getFilename();
        Assert.assertTrue("My File In Section.txt must exist", new File(targetPathSection).exists());

        String targetPathWorkspace = pathPrefix + myWorkspace.getPathAsString() + "/" + blobWorkspace.getFilename();
        Assert.assertTrue("My File In Workspace.txt must exist", new File(targetPathWorkspace).exists());

        String targetPathTemplate = pathPrefix + myTemplate.getPathAsString() + "/" + blobTemplate.getFilename();
        Assert.assertTrue("My File In Template.txt must exist", new File(targetPathTemplate).exists());

        String targetPathFileEmptyWorkspace = pathPrefix + myWorkspaceEmptyName.getPathAsString() + "/"
                + blobWorkspaceEmptyName.getFilename();
        Assert.assertTrue("file in a workspace with empty name.txt must exist",
                new File(targetPathFileEmptyWorkspace).exists());
    }
}
