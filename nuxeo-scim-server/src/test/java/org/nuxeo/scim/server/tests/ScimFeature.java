/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.scim.server.tests;

import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy({"org.nuxeo.usermapper", "org.nuxeo.scim.server"})
public class ScimFeature extends EmbeddedAutomationServerFeature {

}
