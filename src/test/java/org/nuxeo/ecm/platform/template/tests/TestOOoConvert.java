package org.nuxeo.ecm.platform.template.tests;

import java.io.File;

import org.junit.Test;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public class TestOOoConvert extends BaseConverterTest {

    protected static final String ODT_MT = "application/vnd.oasis.opendocument.text";

    @Test
    public void testOfficeConvertert() throws Exception {
        BlobHolder bh = getBlobFromPath("data/testMe.html", "text/html");

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(bh.getBlob().getMimeType(), ODT_MT);
        assertEquals("any2odt", converterName);

        BlobHolder result = cs.convert(converterName, bh, null);

        result.getBlob().transferTo(new File("/tmp/html.odt"));

        bh = getBlobFromPath("data/testMe.md", "text/x-web-markdown");
        assertEquals("any2odt", converterName);

        result = cs.convert(converterName, bh, null);
        result.getBlob().transferTo(new File("/tmp/md.odt"));


    }
}
