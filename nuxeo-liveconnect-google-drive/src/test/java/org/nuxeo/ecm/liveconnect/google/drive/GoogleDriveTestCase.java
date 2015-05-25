/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.liveconnect.google.drive;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@Deploy({ "org.nuxeo.ecm.core.cache", //
})
@LocalDeploy({ "org.nuxeo.ecm.liveconnect.google.drive:OSGI-INF/cache-config.xml",
        "org.nuxeo.ecm.liveconnect.google.drive:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.google.drive:OSGI-INF/test-googledrive-config.xml" })
public class GoogleDriveTestCase {

    protected static final String USERNAME = "tester";

    protected static final String USERID = USERNAME + "@example.com";

    protected static final String FILEID_JPEG = "12341234";

    protected static final String FILEID_DOC = "56785678";

    protected static final int SIZE = 36830;

}
