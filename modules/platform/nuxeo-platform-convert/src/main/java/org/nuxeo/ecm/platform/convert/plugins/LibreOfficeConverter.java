/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.runtime.api.Framework;

/**
 * LibreOffice converter to be used when using the {@code soffice} command line.
 * <p>
 * It fills the {@code -env:userInstallation} argument with a temporary folder to correctly isolate the {@code soffice}
 * processes so multiple instances can be run simultaneously.
 *
 * @since 10.10
 */
public class LibreOfficeConverter extends CommandLineConverter {

    public static final String USER_INSTALLATION_PATH_KEY = "userInstallation";

    private static final Logger log = LogManager.getLogger(LibreOfficeConverter.class);

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        Map<String, String> cmdStringParameters = super.getCmdStringParameters(blobHolder, parameters);

        // create a temporary folder for the user installation env
        try {
            Path tempDirectoryPath = Framework.createTempDirectory(null);
            // the -env:userInstallation expects an URI (file:///tmp/foo)
            cmdStringParameters.put(USER_INSTALLATION_PATH_KEY, tempDirectoryPath.toUri().toString());
        } catch (IOException e) {
            throw new ConversionException(blobHolder, e);
        }

        return cmdStringParameters;
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams) {
        try {
            return super.buildResult(cmdOutput, cmdParams);
        } finally {
            // delete the temp folder
            String userInstallationPath = cmdParams.getParameter(USER_INSTALLATION_PATH_KEY);
            if (userInstallationPath != null) {
                deleteTempDirectory(userInstallationPath);
            }
        }
    }

    private void deleteTempDirectory(String tempFileURI) {
        try {
            // tempFileURI is an URI (file:///tmp/foo)
            URI uri = new URI(tempFileURI);
            File file = new File(uri);
            if (!Files.exists(file.toPath())) {
                return;
            }
            FileUtils.deleteDirectory(file);
        } catch (IOException | URISyntaxException e) {
            log.error(e);
            log.debug(e, e);
        }
    }
}
