package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentParent;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.SaveDocument;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentProperty;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PopDocument;
import org.nuxeo.ecm.automation.core.operations.stack.PushDocument;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ DocumentUpdatePropertiesWithMultiline.InitFeature.class,
        TransactionalFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
// For version label info
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class DocumentUpdatePropertiesWithMultiline {

    public static class InitFeature extends SimpleFeature {

        private static String multilineMode;

        @Override
        public void initialize(FeaturesRunner runner) {
            multilineMode = System.getProperty("nuxeo.automation.properties.multiline.escape");
            System.setProperty("nuxeo.automation.properties.multiline.escape",
                    "true");
        }

        @Override
        public void stop(FeaturesRunner runner) throws Exception {
            if (multilineMode == null) {
                System.getProperties().remove(
                        "nuxeo.automation.properties.multiline.escape");
            } else {
                System.setProperty(
                        "nuxeo.automation.properties.multiline.escape",
                        multilineMode);
            }
        }

    }

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());
    }

    /**
     * Create | GetParent | Update Parent | Save | Pop | Lock.
     */
    @Test
    public void testChain3() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("properties",
                new Properties("dc:title=MyDoc")).set("name", "note");
        chain.add(PushDocument.ID);
        chain.add(GetDocumentParent.ID);
        chain.add(SetDocumentProperty.ID).set("xpath", "dc:description").set(
                "value", "parentdoc");
        chain.add(SaveDocument.ID);
        chain.add(PopDocument.ID);
        chain.add(UpdateDocument.ID).set(
                "properties",
                new Properties("dc:title=MyDoc2\ndc:description="
                        + "mydesc\notherdesc".replace("\n", "\\\n")));
        chain.add(LockDocument.ID);
        chain.add(SaveDocument.ID);

        assertNull(src.getPropertyValue("dc:description"));
        DocumentModel out = (DocumentModel) service.run(ctx, chain);
        assertEquals("mydesc\notherdesc",
                out.getPropertyValue("dc:description"));
        assertEquals("MyDoc2", out.getPropertyValue("dc:title"));
        assertTrue(out.isLocked());
        assertEquals(
                "parentdoc",
                session.getDocument(src.getRef()).getPropertyValue(
                        "dc:description"));
    }
}