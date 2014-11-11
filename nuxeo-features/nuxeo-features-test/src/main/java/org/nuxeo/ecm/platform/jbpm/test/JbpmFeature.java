/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.jbpm.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @author matic
 * 
 */
@Deploy({ "org.nuxeo.ecm.platform.jbpm.api", "org.nuxeo.ecm.platform.jbpm.core", "org.nuxeo.ecm.platform.jbpm.testing" })
@LocalDeploy("org.nuxeo.ecm.platform.jbpm.testing:OSGI-INF/test-contrib.xml")
@Features(PlatformFeature.class)
public class JbpmFeature extends SimpleFeature {

}
