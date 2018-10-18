/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.platform.convert")
public abstract class BaseConverterTest {

    @Inject
    protected ConversionService cs;

    protected static final Log log = LogFactory.getLog(BaseConverterTest.class);

    @Before
    public void setUp() throws Exception {
        assertNotNull(cs);
    }

    protected void checkConverterAvailability(String converterName) {
        ConverterCheckResult check = cs.isConverterAvailable(converterName);
        assertNotNull(check);
        assumeTrue(String.format(
                "Skipping %s tests since commandLine is not installed:\n"
                        + "- installation message: %s\n- error message: %s",
                converterName, check.getInstallationMessage(), check.getErrorMessage()), check.isAvailable());
    }

    protected void checkCommandAvailability(String command) {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability(command);
        assumeTrue(String.format("Convert command %s is not available, skipping test", command), ca.isAvailable());
    }

    protected static BlobHolder getBlobFromPath(String path, String srcMT) throws IOException {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);

        Blob blob = Blobs.createBlob(file);
        if (srcMT != null) {
            blob.setMimeType(srcMT);
        } else {
            MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
            blob.setMimeType(mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(file.getName(), blob, null));
        }
        blob.setFilename(file.getName());
        return new SimpleBlobHolder(blob);
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        return getBlobFromPath(path, null);
    }
}
