/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertFalse;

import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.nuxeo.ecm.core.bulk.AbstractTestFullGCOrphanBlobs;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * @since 2023.5
 */
@Features({ CoreFeature.class, S3BlobProviderFeature.class, LogCaptureFeature.class })
public class TestS3FullGCOrphanBlobsSharedStorage extends AbstractTestFullGCOrphanBlobs {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    public static class FullGCWarnLogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LogEvent event) {
            return event.getLevel().equals(Level.WARN) && event.getLoggerName().contains("DocumentBlobManagerComponent")
                    && event.getMessage().getFormattedMessage().startsWith("Shared storages detected:");
        }
    }

    @Override
    public int getNbFiles() {
        return 2;
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-shared-storage.xml")
    @LogCaptureFeature.FilterWith(FullGCWarnLogFilter.class)
    public void testGCBlobsAction() {
        // because of shared storage, blobs are scrolled twice
        testGCBlobsAction(false, getNbFiles() * 2, sizeOfBinaries * 2);
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertFalse(caughtEvents.isEmpty());
    }

}
