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
 *     Benjamin JALON
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class ArrayStringToListTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    private OperationContext ctx;

    @Before
    public void setup() {
        ctx = new OperationContext(session);
    }

    @Test
    public void shouldAdaptArrayStringAsStringList() throws Exception {
        String[] value = new String[] { "a", "b", };

        Object result = automationService.getAdaptedValue(ctx, value,
                StringList.class);
        assertNotNull(result);
        assertTrue(result instanceof StringList);
        StringList list = (StringList) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));

    }

}
