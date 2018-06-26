package org.nuxeo.ecm.core.io.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.impl.TransactionBatchingDocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.extensions.DocumentLockImporter;
import org.nuxeo.ecm.core.io.impl.plugins.ExtensibleDocumentWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDirectoryReader;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/export-docTypes.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/import-docTypes.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/other-repo.xml")
public class ExportAndTransformAndInportTest extends BaseExport {

    public void runImport(DocumentModel root, File source) throws IOException {

        final DocumentReader reader = new XMLDirectoryReader(source);
        ExtensibleDocumentWriter writer = new ExtensibleDocumentWriter(root.getCoreSession(), root.getPathAsString());

        writer.registerExtension(new DocumentLockImporter());

        DocumentPipe pipe = new TransactionBatchingDocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

    }

    @Test
    public void testExportWithTransformThenImport() throws Exception {

        Principal principal = session.getPrincipal();
        CloseableCoreSession importSession = null;

        DocumentModel root = createSomethingToExport(session);

        File out = getExportDirectory();

        try {
            runExport(root, out, skipBlobs);

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            importSession = CoreInstance.openCoreSession("import", principal);

            runImport(importSession.getRootDocument(), out);

            importSession.save();

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            Thread.sleep(2000);

            DocumentModelList alldocs = importSession.query("select * from Document order by ecm:path");

            StringBuffer sb = new StringBuffer();

            dump(sb, alldocs);

            String listing = sb.toString();

            assertTrue(listing.contains("/ws1/folder/file"));

            // Check that UUIDs are stables
            assertTrue(importSession.exists(root.getRef()));

            // Check versions
            DocumentRef ref = new PathRef("/ws1/folder/file");
            DocumentModel doc = session.getDocument(ref);
            assertNotNull(doc);

            assertEquals("approved", doc.getCurrentLifeCycleState());

            List<DocumentModel> versions = importSession.getVersions(ref);
            assertEquals(2, versions.size());

            // check transtyping for Invoice !
            DocumentModel invoice = importSession.getDocument(new PathRef("/ws1/invoice"));
            assertEquals("File", invoice.getType());
            assertTrue(invoice.hasFacet("Invoice"));
            assertEquals("$10,000", invoice.getPropertyValue("iv:InvoiceAmount"));

            // check field translation
            assertEquals("XYZ", invoice.getPropertyValue("iv:B"));
            String[] lst = (String[]) invoice.getPropertyValue("iv:A");
            assertEquals("A", lst[0]);
            assertEquals("B", lst[1]);

            // check new schema
            assertEquals("foo", invoice.getPropertyValue("nw:Y"));

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();

            // check lock
            assertTrue(invoice.isLocked());

        } finally {
            FileUtils.deleteQuietly(out);
            if (importSession != null) {
                CoreInstance.closeCoreSession(importSession);
            }
        }
    }

}
