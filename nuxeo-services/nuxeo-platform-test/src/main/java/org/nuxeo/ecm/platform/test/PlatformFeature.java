/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.test;

import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Deploy({ "org.nuxeo.ecm.platform.api", "org.nuxeo.ecm.platform.content.template", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.usermanager.api", "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.core.io",
        "org.nuxeo.ecm.platform.query.api", "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml" })
@Features({ CoreFeature.class, ClientLoginFeature.class, DirectoryFeature.class })
public class PlatformFeature extends SimpleFeature {

}
