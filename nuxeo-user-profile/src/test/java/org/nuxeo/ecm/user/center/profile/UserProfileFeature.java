package org.nuxeo.ecm.user.center.profile;
/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 5.9.3
 */
@Features({ TransactionalFeature.class, CoreFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.platform.userworkspace.api",
    "org.nuxeo.ecm.platform.userworkspace.types",
    "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.platform.picture.core",
    "org.nuxeo.ecm.platform.login",
    "org.nuxeo.ecm.platform.web.common",
    "org.nuxeo.ecm.user.center.profile" })
public class UserProfileFeature extends SimpleFeature {

    protected File dir;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        super.start(runner);
        String dirPath = deployDataFiles();
        dir = new File(dirPath);
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (dir != null) {
            FileUtils.deleteDirectory(dir);
        }
        dir = null;
        super.stop(runner);
    }

    protected String deployDataFiles() throws IOException {
        File src = new File(
                org.nuxeo.common.utils.FileUtils.getResourcePathFromContext("data"));
        File dst = File.createTempFile("nuxeoImportTest", ".dir");
        dst.delete();
        dst.mkdir();
        Framework.getProperties().put("nuxeo.userprofile.blobs.folder",
                dst.getPath() + "/data");
        FileUtils.copyDirectoryToDirectory(src, dst);
        return dst.getPath();
    }

}
