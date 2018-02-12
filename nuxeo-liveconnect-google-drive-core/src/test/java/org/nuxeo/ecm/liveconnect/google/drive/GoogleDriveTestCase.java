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

package org.nuxeo.ecm.liveconnect.google.drive;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.cache", //
})
@Deploy({ "org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml",
        "org.nuxeo.ecm.liveconnect.google.drive.core:OSGI-INF/cache-config.xml",
        "org.nuxeo.ecm.liveconnect.google.drive.core:OSGI-INF/core-types-contrib.xml",
        "org.nuxeo.ecm.liveconnect:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.google.drive.core:OSGI-INF/test-googledrive-config.xml",
        "org.nuxeo.ecm.liveconnect.google.drive.core:OSGI-INF/googledrive-pageprovider-contrib.xml" })
public class GoogleDriveTestCase {

    protected static final String USERNAME = "tester";

    protected static final String USERID = USERNAME + "@example.com";

    protected static final String JPEG_FILEID = "12341234";

    protected static final String JPEG_REVID = "v1abcd";

    protected static final int JPEG_SIZE = 36830;

    protected static final int JPEG_REV_SIZE = 18581;

    protected static final String GOOGLEDOC_FILEID = "56785678";

    protected static final String GOOGLEDOC_REVID = "4551";

}
