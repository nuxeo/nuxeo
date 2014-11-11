import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;


@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:complexTypeContribs.xml")
public class TestComplexTypesMapping {

    @Inject
    CoreSession session;

    protected DocumentModel doc;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        doc = session.createDocumentModel("/", "FileTest", "File");
        doc.setPropertyValue("dc:title", "TitleInit");
        Blob blob = new StringBlob("Yo");
        blob.setFilename("Yo.txt");
        blob.setMimeType("text/plain");
        doc.setPropertyValue("file:content", (Serializable)blob);

        List<Map<String, Object>> blobs = new ArrayList<Map<String,Object>>();
        for (int i = 0; i <5 ; i++) {
            Blob subblob = new StringBlob("Yo" + i);
            subblob.setFilename( "File" + i + ".txt");
            subblob.setMimeType("text/plain");

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("file", subblob);
            map.put("filename", "File" + i + ".txt");
            blobs.add(map);
        }
        doc.setPropertyValue("files:files", (Serializable)blobs);

        doc = session.createDocument(doc);

        doc.addFacet("Addresses");

        Map<String, Object> address = new HashMap<String, Object>();
        address.put("streetNumber", "1bis");
        address.put("streetName", "whatever");
        address.put("zipCode", 75020);
        doc.setPropertyValue("addr:addressSingle", (Serializable) address);

        List<Map<String, Object>> addresses = new ArrayList<Map<String,Object>>();
        for (int i=0; i< 5; i++) {
            Map<String, Object> add = new HashMap<String, Object>();
            add.put("streetNumber", "" + i);
            add.put("streetName", "whatever");
            add.put("zipCode", 75000 + i);
            addresses.add(add);
        }
        doc.setPropertyValue("addr:addressesList", (Serializable) addresses);
        doc = session.saveDocument(doc);
        session.save();

        Map<String, Object> addressSingle= (Map<String, Object>) doc.getPropertyValue("addr:addressSingle");
        System.out.println(addressSingle);

     }

    // ------ Tests comes here --------

    @Test
    public void testPropertiesMapping() throws Exception {

        File propFile = FileUtils.getResourceFileFromContext("testProperties.properties");
        Assert.assertNotNull(propFile);

        Properties props = new Properties(Properties.loadProperties(new FileReader(propFile)));

        DocumentHelper.setProperties(session, doc, props);

        doc = session.saveDocument(doc);
        session.save();

        Map<String, Object> address = (Map<String, Object>) doc.getPropertyValue("addr:addressSingle");
        Assert.assertNotNull(address);
        Assert.assertEquals("2bis", address.get("streetNumber"));
        Assert.assertEquals(7654L, address.get("zipCode"));
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) doc.getPropertyValue("addr:addressesList");
        Assert.assertNotNull(addresses);
        Assert.assertEquals(2, addresses.size());
        Assert.assertEquals("3bis", addresses.get(0).get("streetNumber"));
        Assert.assertEquals(2L, addresses.get(0).get("zipCode"));







    }

}
