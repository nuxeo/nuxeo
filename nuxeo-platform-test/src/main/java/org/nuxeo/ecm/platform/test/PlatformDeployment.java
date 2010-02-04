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
package org.nuxeo.ecm.platform.test;

import org.nuxeo.ecm.core.test.CoreDeployment;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Deploy({
    "org.nuxeo.ecm.platform.api",
    "org.nuxeo.ecm.platform.content.template",
    "org.nuxeo.ecm.platform.dublincore",
    "org.nuxeo.ecm.directory",
    "org.nuxeo.ecm.directory.sql",
//    "org.nuxeo.ecm.directory.ldap",
    "org.nuxeo.ecm.platform.usermanager.api",
    "org.nuxeo.ecm.platform.usermanager",
    "org.nuxeo.ecm.platform.test:test-usermanagerimpl/schemas-config.xml",
    "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml"
})
public interface PlatformDeployment extends CoreDeployment {

}
