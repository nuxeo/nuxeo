/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jboss.el.ExpressionFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class TestExtendedInfoEvaluation extends SQLRepositoryTestCase {

    protected ExpressionEvaluator evaluatorUnderTest;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        evaluatorUnderTest = new ExpressionEvaluator(
                new ExpressionFactoryImpl());

        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected DocumentModel doCreateDocument() throws ClientException {
        DocumentModel rootDocument = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");

        return session.createDocument(model);
    }

    @Test
    public void testBean() throws ClientException {
        ExpressionContext context = new ExpressionContext();
        DocumentModel source = doCreateDocument();
        EventContext eventContext = new DocumentEventContext(session,
                session.getPrincipal(), source);
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("test", "test");
        eventContext.setProperties(properties);
        evaluatorUnderTest.bindValue(context, "context", eventContext);
        DocumentModel value = evaluatorUnderTest.evaluateExpression(context,
                "${context.arguments[0]}", DocumentModel.class);
        assertNotNull(value);
        assertEquals(source, value);
        String test = evaluatorUnderTest.evaluateExpression(context,
                "${context.properties.test}", String.class);
        assertNotNull(value);
        assertEquals("test", test);
    }

}
