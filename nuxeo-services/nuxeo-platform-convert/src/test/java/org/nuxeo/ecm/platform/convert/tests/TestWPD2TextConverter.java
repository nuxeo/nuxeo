package org.nuxeo.ecm.platform.convert.tests;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.runtime.api.Framework;

public class TestWPD2TextConverter extends BaseConverterTest {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
    }


    public void testWordPerfectToTextConverter() throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);
        ConverterCheckResult check = cs.isConverterAvailable("wpd2text");
        assertNotNull(check);

        if (!check.isAvailable()) {
            System.out.print("Skipping Wordperfect conversion test since libpwd-tool is not installed");
            System.out.print(" converter check output : " + check.getInstallationMessage());
            System.out.print(" converter check output : " + check.getErrorMessage());
            return;
        }


        String converterName = cs.getConverterName("application/wordperfect", "text/plain");
        assertEquals("wpd2text", converterName);

        BlobHolder hg = getBlobFromPath("test-docs/test.wpd", "application/wordperfect");

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String txt = result.getBlob().getString();
        //System.out.println(txt);
        assertTrue(txt.contains("Zoonotic"));
        assertTrue(txt.contains("Committee"));

    }

}
