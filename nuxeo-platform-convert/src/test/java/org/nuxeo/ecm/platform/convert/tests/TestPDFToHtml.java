package org.nuxeo.ecm.platform.convert.tests;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestPDFToHtml extends NXRuntimeTestCase {

    protected ConversionService cs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.convert");

        cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);

    }

    protected static BlobHolder getBlobFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new SimpleBlobHolder(new FileBlob(file));
    }

    public void testConverter() throws Exception {

        String converterName = cs.getConverterName("application/pdf", "text/html");
        assertEquals("pdf2html", converterName);

        CommandLineExecutorService cles = Framework
                .getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability("pdftohtml");

        if (!ca.isAvailable()) {
            System.out.println("pdftohtml command is not avalaible, skipping test");
            return;
        }

        BlobHolder pdfBH = getBlobFromPath("test-docs/hello.pdf");

        BlobHolder result = cs.convert(converterName, pdfBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(2, blobs.size());
        Blob mainBlob = result.getBlob();
        assertEquals("index.html", mainBlob.getFilename());
        Blob subBlob = blobs.get(1);
        assertTrue(subBlob.getFilename().startsWith("index001"));

        String htmlContent = mainBlob.getString();
        assertTrue(htmlContent.contains("Hello"));

    }

}
