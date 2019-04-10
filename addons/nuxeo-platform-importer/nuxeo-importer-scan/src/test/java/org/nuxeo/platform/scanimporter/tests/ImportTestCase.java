/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.platform.scanimporter.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.importer.core")
@Deploy("org.nuxeo.ecm.platform.scanimporter")
public abstract class ImportTestCase {

    @Inject
    protected CoreSession session;

    protected List<File> tmp = new ArrayList<File>();

    protected String deployTestFiles(String name) throws IOException {
        File src = new File(org.nuxeo.common.utils.FileUtils.getResourcePathFromContext("data/" + name));
        File dst = Framework.createTempFile("nuxeoImportTestCase", ".dir");
        dst.delete();
        dst.mkdir();
        Framework.getProperties().put("nuxeo.import.tmpdir", dst.getPath());
        tmp.add(dst);
        FileUtils.copyDirectoryToDirectory(src, dst);
        return dst.getPath() + File.separator + name;
    }

    @After
    public void tearDown() throws Exception {
        for (File dir : tmp) {
            if (dir.exists()) {
                FileUtils.deleteDirectory(dir);
            }
        }
        tmp.clear();
    }

}
