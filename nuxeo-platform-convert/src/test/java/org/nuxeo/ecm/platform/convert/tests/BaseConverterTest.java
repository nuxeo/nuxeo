package org.nuxeo.ecm.platform.convert.tests;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

public abstract class BaseConverterTest extends NXRuntimeTestCase {

    protected static BlobHolder getBlobFromPath(String path, String srcMT) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        Blob blob = new FileBlob(file);
        if (srcMT!=null) {
            blob.setMimeType(srcMT);
        }
        blob.setFilename(file.getName());
        return new SimpleBlobHolder(blob);
    }

    protected static BlobHolder getBlobFromPath(String path) {
        return getBlobFromPath(path, null);
    }



    public BaseConverterTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
           super.setUp();
           deployBundle("org.nuxeo.ecm.core.api");
           deployBundle("org.nuxeo.ecm.core.convert.api");
           deployBundle("org.nuxeo.ecm.core.convert");
           deployBundle("org.nuxeo.ecm.platform.mimetype.api");
           deployBundle("org.nuxeo.ecm.platform.mimetype.core");
           deployBundle("org.nuxeo.ecm.platform.convert");
    }

    public BaseConverterTest(String name) {
        super(name);
    }

    public static String readPdfText(File pdfFile) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        PDDocument document = PDDocument.load(pdfFile);
        String text = textStripper.getText(document);
        document.close();
        return text.trim();
    }




}