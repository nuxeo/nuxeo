/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.binary.metadata.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.After;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;

/**
 * @since 11.1
 */
public abstract class BaseBinaryMetadataTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected BinaryMetadataService binaryMetadataService;

    @Inject
    protected CommandLineExecutorService commandLineExecutorService;

    @After
    public void itShouldFailIfOriginalFileIsCreatedByExifTool() throws IOException {
        CmdParameters defaultCmdParameters = commandLineExecutorService.getDefaultCmdParameters();
        Path tmpDirectory = Paths.get(defaultCmdParameters.getParameter(Environment.NUXEO_TMP_DIR));

        try (Stream<Path> stream = Files.find(tmpDirectory, 1, (p, bf) -> p.toFile().getName().endsWith("_original"))) {
            assertFalse(stream.findAny().isPresent());
        }

    }
}
