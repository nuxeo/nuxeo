/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.automation.test;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Based on the existing {@link PlatformFeature}, AutomationFeature is a simple
 * feature that includes org.nuxeo.ecm.automation.core and
 * org.nuxeo.ecm.automation.features bundles.
 *
 * @since 5.7
 * @since 5.6-HF02
 *
 */
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features" })
public class AutomationFeature extends SimpleFeature {
}
