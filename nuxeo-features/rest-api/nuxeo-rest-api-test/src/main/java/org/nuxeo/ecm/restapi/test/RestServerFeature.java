/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 5.7.2
 */
@Features({ RuntimeFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.test", "org.nuxeo.ecm.platform.url.api",
        "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.automation.io",
        "org.nuxeo.ecm.platform.restapi.io", "org.nuxeo.ecm.platform.restapi.test",
        "org.nuxeo.ecm.platform.restapi.server", "org.nuxeo.ecm.platform.tag"
         })
public class RestServerFeature extends SimpleFeature {

}
