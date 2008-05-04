/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.uidgen;

import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * @author DM
 *
 */
public abstract class UIDGenBaseTestCase extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the core bundle
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreService.xml");
        deployContrib("org.nuxeo.ecm.core.jcr-connector", "TypeService.xml");
        deployContrib("org.nuxeo.ecm.core.jcr-connector", "SecurityService.xml");
        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.core.jcr-connector",
                "test-CoreExtensions.xml");

        deployContrib("org.nuxeo.ecm.core.jcr-connector", "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "CoreEventListenerService.xml");
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/LifeCycleService.xml");

        // UID specific
        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "nxuidgenerator-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "nxuidgenerator-bundle-contrib.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
