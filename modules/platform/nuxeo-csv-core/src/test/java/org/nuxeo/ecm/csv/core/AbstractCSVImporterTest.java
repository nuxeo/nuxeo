/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.csv.core;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.transientstore.test.TransientStoreFeature;

@Features({ CoreFeature.class, DirectoryFeature.class, UserManagerFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.ecm.csv.core")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-directories-contrib.xml")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-types-contrib.xml")
@Deploy("org.nuxeo.ecm.csv.core:OSGI-INF/test-ui-types-contrib.xml")
public abstract class AbstractCSVImporterTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected CSVImporter csvImporter;

    @Inject
    protected WorkManager workManager;

    protected Blob getCSVBlob(String name) throws IOException {
        Blob blob = Blobs.createBlob(new File(FileUtils.getResourcePathFromContext(name)));
        blob.setFilename(name);
        return blob;
    }
}
