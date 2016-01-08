/*
 * (C) Copyright 2012-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     jcarsique
 */
package org.nuxeo.connect.update.standalone;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public abstract class PackageTestCase {

    protected static final Log log = LogFactory.getLog(PackageTestCase.class);

    protected PackageUpdateService service;

    /**
     * Calls {@link #setupService()} to setup the service
     *
     * @see #setupService()
     */
    @Before
    public void setUp() throws Exception {
        setupService();
    }

    @After
    public void tearDown() throws Exception {
        if (service instanceof StandaloneUpdateService) {
            tearDownStandaloneUpdateService();
        }
    }

    /**
     * Default implementation sets a {@link StandaloneUpdateService}
     *
     * @throws IOException
     * @throws PackageException
     */
    protected void setupService() throws IOException, PackageException {
        File tmpHome = Framework.createTempFile("tmphome", null);
        Framework.trackFile(tmpHome, tmpHome);
        FileUtils.forceDelete(tmpHome);
        tmpHome.mkdirs();
        Environment env = new Environment(tmpHome);
        Environment.setDefault(env);
        env.setServerHome(tmpHome);
        env.init();
        service = new StandaloneUpdateService(env);
        service.initialize();
        File storeDir = ((StandaloneUpdateService) service).getPersistence().getStore();
        File junkPackageFile = File.createTempFile("junk", null, storeDir);
        junkPackageFile.deleteOnExit();
    }

    protected void tearDownStandaloneUpdateService() {
        FileUtils.deleteQuietly(Environment.getDefault().getHome());
    }

}
