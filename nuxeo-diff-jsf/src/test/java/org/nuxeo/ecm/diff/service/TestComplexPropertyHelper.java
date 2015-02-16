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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.service.ComplexPropertyHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the ComplexPropertyHelper class. TODO: add tests
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.diff.test" })
public class TestComplexPropertyHelper {

    @Inject
    protected CoreSession session;

    @Test
    public void testGetField() throws ClientException {

        Field field = ComplexPropertyHelper.getField("simpletypes", "string");
        assertNotNull(field);
        assertEquals("string", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());

        field = ComplexPropertyHelper.getField("simpletypes", "textarea");
        assertNotNull(field);
        assertEquals("textarea", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
    }
}
