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

    public void testMap() throws DocumentException, AuditException {
        ExpressionContext context = new ExpressionContext();
        evaluatorUnderTest.bindValue(context, "message", message);
        Serializable value = (Serializable) evaluatorUnderTest.evaluateExpression(
                context, "${message.id}", Serializable.class);
        assertNotNull(value);
        assertTrue(value instanceof String);
        String id = (String) value;
        assertEquals(message.getId(), id);
    }
    
    public void testArray() throws DocumentException, AuditException {
        ExpressionContext context = new ExpressionContext();
        evaluatorUnderTest.bindValue(context, "message", message);
        Serializable value = (Serializable) evaluatorUnderTest.evaluateExpression(
                context, "${message.id}", Serializable.class);
        assertNotNull(value);
        assertTrue(value instanceof String);
        String id = (String) value;
        assertEquals(message.getId(), id);
    }
    
    public void testProperty() throws DocumentException, AuditException {
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
