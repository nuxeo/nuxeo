/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.test;

import org.nuxeo.ecm.platform.test.PlatformDeployment;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Deploy({
    "org.nuxeo.ecm.platform.login",
    "org.nuxeo.ecm.platform.web.common",
    "org.nuxeo.ecm.webengine.admin",
    "org.nuxeo.ecm.webengine.base",
    "org.nuxeo.ecm.webengine.core",
    "org.nuxeo.ecm.webengine.resteasy.adapter",
    "org.nuxeo.runtime.jetty",
    "org.nuxeo.ecm.webengine.ui",
    "org.nuxeo.theme.core",
    "org.nuxeo.theme.html",
    "org.nuxeo.theme.fragments",
    "org.nuxeo.theme.webengine",
    "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml",
    "org.nuxeo.ecm.webengine.test:authentication-config.xml",
    "org.nuxeo.ecm.webengine.test:login-anonymous-config.xml",
    "org.nuxeo.ecm.webengine.test:login-config.xml",    
    "org.nuxeo.ecm.webengine.test:runtimeserver-contrib.xml"
})
public interface WebEngineDeployment extends PlatformDeployment {

}
