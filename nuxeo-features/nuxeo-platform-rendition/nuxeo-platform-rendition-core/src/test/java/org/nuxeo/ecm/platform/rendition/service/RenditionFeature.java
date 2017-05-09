/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 7.3
 */
@Features({ CoreFeature.class, DirectoryFeature.class, TransientStoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.convert", //
        "org.nuxeo.ecm.platform.login", //
        "org.nuxeo.ecm.platform.web.common", //
        "org.nuxeo.ecm.platform.usermanager.api", //
        "org.nuxeo.ecm.platform.usermanager:OSGI-INF/UserService.xml", //
        "org.nuxeo.ecm.actions", //
        "org.nuxeo.ecm.platform.rendition.api", //
        "org.nuxeo.ecm.platform.rendition.core", //
        "org.nuxeo.ecm.automation.core", //
        "org.nuxeo.ecm.platform.io.core", //
        "org.nuxeo.ecm.platform.dublincore" //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml", //
        "org.nuxeo.ecm.platform.rendition.core:test-directories-contrib.xml", //
        "org.nuxeo.ecm.platform.rendition.core:test-automation-contrib.xml" //
})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class RenditionFeature extends SimpleFeature {
}
