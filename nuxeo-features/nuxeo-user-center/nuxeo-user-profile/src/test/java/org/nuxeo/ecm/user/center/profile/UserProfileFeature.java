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
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 7.2
 */
@Features(PlatformFeature.class)
// @RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.api")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.user.center.profile")
public class UserProfileFeature implements RunnerFeature {

    protected File dir;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        String dirPath = deployDataFiles();
        dir = new File(dirPath);
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (dir != null) {
            FileUtils.deleteDirectory(dir);
        }
        dir = null;
    }

    protected String deployDataFiles() throws IOException {
        File src = new File(org.nuxeo.common.utils.FileUtils.getResourcePathFromContext("data"));
        File dst = Framework.createTempFile("nuxeoImportTest", ".dir");
        dst.delete();
        dst.mkdir();
        Framework.getProperties().setProperty(UserProfileImporter.BLOB_FOLDER_PROPERTY, dst.getPath() + "/data");
        FileUtils.copyDirectoryToDirectory(src, dst);
        return dst.getPath();
    }

}
