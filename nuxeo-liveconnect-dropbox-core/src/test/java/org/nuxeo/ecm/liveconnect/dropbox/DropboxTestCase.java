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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.liveconnect.dropbox;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.cache", "org.nuxeo.ecm.core.mimetype", "org.nuxeo.ecm.platform.oauth" })
@Deploy({ "org.nuxeo.ecm.liveconnect.dropbox:OSGI-INF/cache-config.xml",
        "org.nuxeo.ecm.liveconnect.dropbox:OSGI-INF/test-dropbox-config.xml","org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml",
        "org.nuxeo.ecm.liveconnect:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.dropbox:OSGI-INF/dropbox-pageprovider-contrib.xml" })
public class DropboxTestCase {

    protected static final String USERID = "tester@example.com";

    protected static final String FILEID_JPEG = "12341234";

    protected static final String FILEID_DOC = "56785678";

    protected static final int SIZE = 36830;

}
