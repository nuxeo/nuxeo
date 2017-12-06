package org.nuxeo.ecm.core.io.transform;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.impl.TransactionBatchingDocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.extensions.LockExporterExtension;
import org.nuxeo.ecm.core.io.impl.extensions.VersionInfoExportExtension;
import org.nuxeo.ecm.core.io.impl.plugins.ExtensibleDocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDirectoryWriter;
import org.nuxeo.ecm.core.io.impl.transformers.DoctypeToFacetTranslator;
import org.nuxeo.ecm.core.io.impl.transformers.FacetRemover;
import org.nuxeo.ecm.core.io.impl.transformers.FieldMapper;
import org.nuxeo.ecm.core.io.impl.transformers.SchemaRemover;
import org.nuxeo.ecm.core.io.impl.transformers.SchemaRenamer;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class BaseExport {

    @Inject
    protected CoreSession session;
    String uuid;
    protected boolean skipBlobs = true;
    public static final String IODIR = "NX-Export-Import";

    protected DocumentModel createSomethingToExport(CoreSession session) throws Exception {

        DocumentModel rootDocument = session.getRootDocument();

        DocumentModel workspace = session.createDocumentModel(rootDocument.getPathAsString(), "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

        DocumentModel invoiceDoc = session.createDocumentModel(workspace.getPathAsString(), "invoice", "Invoice");
        invoiceDoc.setProperty("dublincore", "title", "MyDoc");
        invoiceDoc.setProperty("invoice", "InvoiceNumber", "0001");
        invoiceDoc.setPropertyValue("inv:InvoiceAmount", "$10,000");

        invoiceDoc.setPropertyValue("dep:fieldA", new String[] { "A", "B", "C" });
        invoiceDoc.setPropertyValue("dep:fieldB", "XYZ");
        invoiceDoc.setPropertyValue("dep:fieldC", "foo");
        invoiceDoc.setPropertyValue("dep:fieldD", "bar");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        invoiceDoc.setProperty("file", "content", blob);

        invoiceDoc = session.createDocument(invoiceDoc);

        invoiceDoc.addFacet("HiddenInNavigation");
        invoiceDoc = session.saveDocument(invoiceDoc);

        DocumentModel folderDoc = session.createDocumentModel(workspace.getPathAsString(), "folder", "Folder");
        folderDoc.setProperty("dublincore", "title", "MyFolder");
        folderDoc = session.createDocument(folderDoc);

        DocumentModel fileDoc2 = session.createDocumentModel(folderDoc.getPathAsString(), "file", "File");
        fileDoc2.setProperty("dublincore", "title", "MyDoc");

        Blob blob2 = new StringBlob("SomeDummyContent2");
        blob.setFilename("dummyBlob2.txt");
        fileDoc2.setProperty("file", "content", blob2);

        fileDoc2 = session.createDocument(fileDoc2);

        fileDoc2.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        fileDoc2.setPropertyValue("dc:description", "Youhou");
        fileDoc2 = session.saveDocument(fileDoc2);

        fileDoc2.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        fileDoc2.setPropertyValue("dc:description", "Youhou2");
        fileDoc2 = session.saveDocument(fileDoc2);

        fileDoc2.followTransition("approve");

        assertEquals("approved", fileDoc2.getCurrentLifeCycleState());

        uuid = fileDoc2.getId();

        // lock the document
        invoiceDoc.setLock();

        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Audit being async we must wait !
        Thread.sleep(200);
        EventService es = Framework.getService(EventService.class);
        es.waitForAsyncCompletion();

        return workspace;
    }

    public BaseExport() {
        super();
    }

    protected File getExportDirectory() {
        return getExportDirectory(true);
    }

    protected File getExportDirectory(boolean delete) {

        String tempDir = System.getProperty("java.io.tmpdir");

        File dir = new File(tempDir + "/" + IODIR);

        if (dir.exists()) {
            if (delete) {
                FileUtils.deleteQuietly(dir);
            } else {
                return dir;
            }
        }
        dir.mkdirs();
        return dir;
    }

    protected void runExport(DocumentModel root, File destination, boolean skipBlobs) throws Exception {

        final ExtensibleDocumentTreeReader reader = new ExtensibleDocumentTreeReader(root.getCoreSession(), root);
        XMLDirectoryWriter writer = new XMLDirectoryWriter(destination);
        writer.setSkipBlobs(skipBlobs);

        // register extensions !
        reader.registerExtension(new VersionInfoExportExtension());
        reader.registerExtension(new LockExporterExtension());

        DocumentPipe pipe = new TransactionBatchingDocumentPipeImpl(10);

        pipe.setReader(reader);
        pipe.setWriter(writer);

        pipe.addTransformer(new DoctypeToFacetTranslator("Invoice", "File", "Invoice"));
        pipe.addTransformer(new FacetRemover(null, "IOnlyExistsInV1"));
        pipe.addTransformer(new FacetRemover(null, "Immutable"));
        pipe.addTransformer(new FieldMapper("deprecated", "dep:fieldA", "invoice", "inv:A"));
        pipe.addTransformer(new FieldMapper("deprecated", "dep:fieldB", "invoice", "inv:B"));
        pipe.addTransformer(new FieldMapper("deprecated", "dep:fieldC", "new", "nw:Y"));
        pipe.addTransformer(new SchemaRemover(null, "deprecated"));
        pipe.addTransformer(new SchemaRenamer("invoice", "invoiceNew", "iv"));

        pipe.run();
    }

    protected void dump(StringBuffer sb, File root) {
        for (File f : root.listFiles()) {
            sb.append(f.getAbsolutePath());
            sb.append("\n");
            if (f.isDirectory()) {
                dump(sb, f);
            }
        }
    }

    protected void dump(StringBuffer sb, DocumentModelList alldocs) {
        for (DocumentModel doc : alldocs) {
            sb.append(doc.getId());
            sb.append(" - ");
            sb.append(doc.getPathAsString());
            sb.append(" - ");
            sb.append(doc.getType());
            sb.append(" - ");
            sb.append(doc.getTitle());
            sb.append(" - ");
            sb.append(doc.isVersion());
            sb.append(" - ");
            sb.append(doc.getVersionLabel());
            sb.append("\n");


        }
    }


}