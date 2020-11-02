/*
 * (C) Copyright 2011-2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @author dmetzler
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.quota")
@Deploy("org.nuxeo.ecm.quota.test")
@Deploy("org.nuxeo.ecm.platform.content.template")
public class QuotaFeature implements RunnerFeature {

    @SuppressWarnings("unchecked")
    public static <B extends Blob & Serializable> B createFakeBlob(int size) {
        Blob blob = Blobs.createBlob("a".repeat(Math.max(0, size)));
        blob.setFilename("FakeBlob_" + size + ".txt");
        return (B) blob;
    }

    public static void assertQuota(DocumentModel doc, long innerSize, long totalSize) {
        assertQuota(doc, innerSize, totalSize, 0, 0);
    }

    public static void assertQuota(DocumentModel doc, long innerSize, long totalSize, long trashSize) {
        assertQuota(doc, innerSize, totalSize, trashSize, 0);
    }

    public static void assertQuota(DocumentModel doc, long innerSize, long totalSize, long trashSize,
            long versionsSize) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:", innerSize, qa.getInnerSize());
        assertEquals("total:", totalSize, qa.getTotalSize());
        assertEquals("trash:", trashSize, qa.getTrashSize());
        assertEquals("versions: ", versionsSize, qa.getVersionsSize());
    }

}
