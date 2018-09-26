/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.io.fsexporter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.nuxeo.io.fsexporter.test.DocumentBuilder.file;
import static org.nuxeo.io.fsexporter.test.DocumentBuilder.folder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.io.fsexporter.FSExporterService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-fsexporter" })
@Deploy("nuxeo-fsexporter:drivelike-contrib.xml")
public class TestDriveLikeExporterPlugin {

    @Inject
    CoreSession session;

    @Inject
    FSExporterService service;

    @Before
    public void doBefore() throws Exception {
        PathRef defaultDomain = new PathRef("/default-domain");
        if (session.exists(defaultDomain)) {
            session.removeDocument(defaultDomain);
        }
    }

    @Test
    public void it_uses_title_to_serialize_folder() throws Exception {

        // Given a folder with a title
        DocumentModel folder = folder().at("/").withName("folder1").withTitle("Title of folder 1").create(session);
        file().at(folder.getPathAsString())
              .withName("file1")
              .withTitle("Title of file 1")
              .withContent("file1.txt", "test")
              .create(session);
        session.save();

        // When I export the repository
        File exportRoot = exportRepository();

        // Then the exported folder uses the title
        List<File> files = Arrays.asList(exportRoot.listFiles());
        assertThat(files).hasSize(1);

        File expectedFolder = files.get(0);
        assertThat(expectedFolder).hasName("Title of folder 1");

        // Then the exported file uses the filename
        files = Arrays.asList(expectedFolder.listFiles());
        assertThat(files).hasSize(1);

        File expectedFile = files.get(0);
        assertThat(expectedFile).hasName("file1.txt");

    }

    @Test
    public void it_fallbacks_to_name_when_there_is_no_title() throws Exception {
        // Given a folder with no title
        DocumentModel doc = folder().at("/").withName("folder1").create(session);
        doc.setPropertyValue("dc:title", "");
        assertThat(doc.getTitle()).isNullOrEmpty();
        session.saveDocument(doc);
        session.save();

        // When I export the repository
        File exportRoot = exportRepository();

        // Then the exported file uses the filename
        List<File> files = Arrays.asList(exportRoot.listFiles());
        assertThat(files).hasSize(1);

        File expectedFolder = files.get(0);
        assertThat(expectedFolder).hasName("folder1");

    }

    @Test
    public void it_uses_alternate_names_when_there_are_folder_title_collision() throws Exception {
        // Given two folders with same title
        folder().at("/").withName("folder1").withTitle("Title of folder 1").create(session);
        folder().at("/").withName("folder2").withTitle("Title of folder 1").create(session);
        session.save();

        // When I export the repository
        File exportRoot = exportRepository();

        List<String> folderNames = Arrays.asList(exportRoot.listFiles()).stream().map(f -> f.getName()).collect(
                Collectors.toList());

        // Then folder titles are created to not collide
        assertThat(folderNames).hasSize(2);
        assertThat(folderNames).containsOnly("Title of folder 1", "Title of folder 1_1");
    }

    @Test
    public void it_uses_alternate_names_when_there_are_file_title_collision_keeping_extensions() throws Exception {
        // Given two folders with same title
        file().at("/").withName("file1").withTitle("Title of file 1").withContent("file1.txt", "test1").create(session);
        file().at("/").withName("file2").withTitle("Title of file 2").withContent("file1.txt", "test2").create(session);
        session.save();

        // When I export the repository
        File exportRoot = exportRepository();

        List<String> folderNames = Arrays.asList(exportRoot.listFiles()).stream().map(f -> f.getName()).collect(
                Collectors.toList());

        // Then files titles are created to not collide
        assertThat(folderNames).hasSize(2);
        assertThat(folderNames).containsOnly("file1.txt", "file1_1.txt");
    }

    @Test
    public void it_exports_only_the_main_blob_of_a_file() throws Exception {
        // Given a file document with several blobs
        file().at("/")
              .withName("file1")
              .withTitle("Title of file 1")
              .withContent("file1.txt", "test")
              .withAdditionalContent("file2.txt", "test2")
              .create(session);
        session.save();

        // When I export the repository
        File exportRoot = exportRepository();
        List<String> fileNames = Arrays.asList(exportRoot.listFiles()).stream().map(f -> f.getName()).collect(
                Collectors.toList());

        // Then I get only one file
        assertThat(fileNames).hasSize(1);
        assertThat(fileNames).containsExactly("file1.txt");

    }

    private File exportRepository() throws IOException {
        File exportRoot = new File(Environment.getDefault().getTemp().getPath() + "/export");
        FileUtils.deleteDirectory(exportRoot);
        service.export(session, "/", exportRoot.getAbsolutePath(), "");
        return exportRoot;
    }

}
