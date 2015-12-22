/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:kleturc@nuxeo.com">Kevin Leturc</a>
 *
 */
package org.nuxeo.ecm.liveconnect.box;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.cache", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml",
        "org.nuxeo.ecm.liveconnect.box:OSGI-INF/cache-config.xml",
        "org.nuxeo.ecm.liveconnect.box:OSGI-INF/core-types-contrib.xml",
        "org.nuxeo.ecm.liveconnect:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.box:OSGI-INF/test-box-config.xml",
        "org.nuxeo.ecm.liveconnect.box:OSGI-INF/box-pageprovider-contrib.xml" })
public class BoxTestCase {

}
