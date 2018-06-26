package org.nuxeo.ecm.core.io.transform;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/export-docTypes.xml")
public class ExportAndTransformTest extends BaseExport {

    @Test
    public void testExportWithTransform() throws Exception {

        DocumentModel root = createSomethingToExport(session);

        DocumentModelList versions = session.query("select * from Document where ecm:isVersion = 1");

        assertEquals(2, versions.size());

        File out = getExportDirectory();

        try {
            runExport(root, out, skipBlobs);

            StringBuffer sb = new StringBuffer();

            dump(sb, out);

            String listing = sb.toString();

            // check file exported
            assertTrue(listing.contains(
                    "ws1" + File.separator + "folder" + File.separator + "file" + File.separator + "document.xml"));

            // check version exported
            assertTrue(listing.contains("ws1" + File.separator + "folder" + File.separator + "file" + File.separator
                    + "__versions__" + File.separator + "1.0" + File.separator + "document.xml"));

            // check invoice exported
            assertTrue(listing.contains("ws1" + File.separator + "invoice" + File.separator + "document.xml"));
            String xml = FileUtils.readFileToString(
                    new File(out, "ws1" + File.separator + "invoice" + File.separator + "document.xml"), UTF_8);

            assertTrue(xml.contains("<type>File</type>"));
            assertTrue(xml.contains("<facet>Invoice</facet>"));

            // check schema rename
            assertTrue(xml.contains("name=\"invoiceNew\">"));

            // check field translation
            assertTrue(xml.contains("<iv:A>"));
            assertTrue(xml.contains("<iv:B>XYZ"));

            // check schema deleted
            assertFalse(xml.contains("deprecated"));

            // check new Schena
            assertTrue(xml.contains("<schema name=\"new\""));
            assertTrue(xml.contains("<Y>foo</Y>"));

            // check lock info
            assertTrue(xml.contains("<lockInfo"));
            assertTrue(xml.contains("<owner>Administrator</owner>"));
        } finally {
            FileUtils.deleteQuietly(out);
        }
    }

}
