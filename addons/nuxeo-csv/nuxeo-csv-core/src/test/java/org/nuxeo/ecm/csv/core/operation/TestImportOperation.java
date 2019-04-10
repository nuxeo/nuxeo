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
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.csv.core.operation;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, TransientStoreFeature.class })
@Deploy({ "org.nuxeo.ecm.csv.core" })

public class TestImportOperation {

    private static final String DOCS_OK_CSV = "docs_ok.csv";

    @Inject
    private CoreSession mSession;

    private CSVImportOperation mOperation;

    @Before
    public void setup() {
        mOperation = new CSVImportOperation();
        mOperation.mPath = mSession.getRootDocument().getPathAsString();
        mOperation.mSession = mSession;
    }

    @Test
    public void testImportOperation() throws OperationException {
        File csv = FileUtils.getResourceFileFromContext(DOCS_OK_CSV);
        Blob blob = new FileBlob(csv);
        String res = mOperation.importCSV(blob);
        assertNotNull(res);
    }
}
