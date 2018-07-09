/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

@Features({ CoreFeature.class, ClientLoginFeature.class })
@RepositoryConfig(init = CoreDirectoryInit.class)
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/types-config.xml")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/core-directory-config.xml")
public class CoreDirectoryFeature implements RunnerFeature {

    public static final String CORE_DIRECTORY_NAME = "userCoreDirectory";

    public static String USER1_NAME = "user_1";

    public static String USER2_NAME = "user_2";

    public static String USER3_NAME = "user_3";

    protected CoreSession coreSession;

    protected static final Log log = LogFactory.getLog(CoreDirectoryFeature.class);

    @Override
    public void configure(final FeaturesRunner runner, Binder binder) {
        bindDirectory(binder, CORE_DIRECTORY_NAME);
    }

    protected void bindDirectory(Binder binder, final String name) {
        binder.bind(Directory.class).annotatedWith(Names.named(name)).toProvider(new Provider<Directory>() {
            Directory dir;

            @Override
            public Directory get() {
                if (dir == null) {
                    DirectoryService directoryService = Framework.getService(DirectoryService.class);
                    dir = directoryService.getDirectory(name);
                    if (dir == null) {
                        log.error("Unable to find Directory " + name);
                    }
                }
                return dir;
            }
        });
    }

}
