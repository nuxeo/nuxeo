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

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.ecm.platform.events.DocumentMessageFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

import com.sun.el.ExpressionFactoryImpl;

public class TestExtendedInfoEvaluation extends RepositoryTestCase {

    protected Document root;

    protected Document document;

    protected DocumentMessage message;

    protected ExpressionEvaluator evaluatorUnderTest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Session session = getRepository().getSession(null);
        root = session.getRootDocument();
        document = root.addChild("doc", "File");
        message = DocumentMessageFactory.createDocumentMessage(document);
        evaluatorUnderTest = new ExpressionEvaluator(
                new ExpressionFactoryImpl());
    }

    public void testBean() throws DocumentException, AuditException {
        ExpressionContext context = new ExpressionContext();
        evaluatorUnderTest.bindValue(context, "message", message);
        Serializable value = (Serializable) evaluatorUnderTest.evaluateExpression(
                context, "${message.id}", Serializable.class);
        assertNotNull(value);
        assertTrue(value instanceof String);
        String id = (String) value;
        assertEquals(message.getId(), id);
    }


}
