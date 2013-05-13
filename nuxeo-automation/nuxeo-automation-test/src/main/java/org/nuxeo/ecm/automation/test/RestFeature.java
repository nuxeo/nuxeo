/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.automation.test;

import org.nuxeo.runtime.test.runner.Features;

/**
 * Shortcut to deploy bundles required by automation in your test.
 *
 * @deprecated in 5.7: use EmbeddedAutomationServerFeature directly instead.
 */
@Deprecated
@Features({ EmbeddedAutomationServerFeature.class })
public class RestFeature {

}
