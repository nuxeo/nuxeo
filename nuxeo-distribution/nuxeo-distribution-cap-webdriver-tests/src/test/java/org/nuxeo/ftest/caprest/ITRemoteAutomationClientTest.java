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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ftest.caprest;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.test.AbstractAutomationClientTest;
import org.nuxeo.ecm.automation.test.RemoteAutomationServerFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(RemoteAutomationServerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class ITRemoteAutomationClientTest extends AbstractAutomationClientTest {

}
