/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.nuxeo.ecm.core.storage.sql.AzureBinaryManager.ACCOUNT_KEY_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.AzureBinaryManager.ACCOUNT_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.AzureBinaryManager.CONTAINER_PROPERTY;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * WARNING: You must pass those variables to your test configuration: -Dnuxeo.storage.azure.account.name: Azure account
 * name -Dnuxeo.storage.azure.account.key: Azure account key -Dnuxeo.storage.azure.container: A test container name
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestAzureBinaryManager {

    private static Map<String, String> properties = new HashMap<>();

    private AzureBinaryManager binaryManager;

    @BeforeClass
    public static void initialize() {
        Arrays.asList(ACCOUNT_KEY_PROPERTY, ACCOUNT_NAME_PROPERTY, CONTAINER_PROPERTY).forEach(s -> {
            properties.put(s, Framework.getProperty(AzureBinaryManager.getPropertyKey(s)));
        });

        assumeNotNull(properties.get(ACCOUNT_KEY_PROPERTY), properties.get(ACCOUNT_NAME_PROPERTY),
                properties.get(CONTAINER_PROPERTY));
    }

    @Before
    public void setUp() throws IOException {
        binaryManager = new AzureBinaryManager();
        binaryManager.initialize("azuretest", properties);
    }

    @Test
    public void testSomething() {
        assertTrue(true);
    }

}
