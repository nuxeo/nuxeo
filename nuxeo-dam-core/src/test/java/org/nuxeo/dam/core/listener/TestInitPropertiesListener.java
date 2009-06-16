package org.nuxeo.dam.core.listener;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.dam.api.Constants;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestInitPropertiesListener extends SQLRepositoryTestCase {

    public TestInitPropertiesListener() {
        super("TestInitPropertiesListener");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.dam.core");

        openSession();
    }

    public void testListener() throws Exception {
        // Import set document
        DocumentModel importSet = session.createDocumentModel("/",
                "importSetTest", Constants.IMPORT_SET_TYPE);
        importSet.setPropertyValue("damc:author", "testCreator");
        Calendar cal = GregorianCalendar.getInstance();
        importSet.setPropertyValue("damc:authoringDate", cal);
        importSet = session.createDocument(importSet);
        assertNotNull(importSet);
        session.saveDocument(importSet);
        session.save();
        assertTrue(importSet.hasFacet("SuperSpace"));

        // File document
        DocumentModel file = session.createDocumentModel(
                importSet.getPathAsString(), "fileTest", "File");
        file = session.createDocument(file);
        assertNotNull(file);
        session.saveDocument(file);
        session.save();
        assertTrue(file.hasSchema("dam_common"));
        assertEquals(file.getPropertyValue("damc:author"), "testCreator");
        assertEquals(file.getPropertyValue("damc:authoringDate"), cal);

        // Picture document
        DocumentModel picture = session.createDocumentModel(
                importSet.getPathAsString(), "pictureTest", "Picture");
        picture = session.createDocument(picture);
        assertNotNull(picture);
        session.saveDocument(picture);
        session.save();
        assertTrue(picture.hasSchema("dam_common"));
        assertEquals(picture.getPropertyValue("damc:author"), "testCreator");
        assertEquals(picture.getPropertyValue("damc:authoringDate"), cal);
    }

    @Override
    public void tearDown() throws Exception {
        closeSession(session);
        super.tearDown();
    }

}
