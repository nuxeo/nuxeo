package org.nuxeo.ecm.directory.sql;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryManager;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class TestIntIdField extends SQLDirectoryTestCase {

    @Override
    public void setUp() throws Exception {

        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests", "intIdDirectory-contrib.xml");

    }

    public void testIntIdDirectory() throws Exception {


        DirectoryServiceImpl dirServiceImpl =
            (DirectoryServiceImpl) Framework.getRuntime().getComponent(DirectoryService.NAME);

        Session session = dirServiceImpl.open("testIdDirectory");

        assertNotNull(session);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", 1);
        map.put("label", "toto");
        DocumentModel entry = session.createEntry(map);
        assertNotNull(entry);

        map.put("id", 2);
        map.put("label", "titi");
        DocumentModel entry2 = session.createEntry(map);
        assertNotNull(entry2);
        session.commit();

        assertNotNull(session.getEntry("1"));
        assertNotNull(session.getEntry("2"));
        assertNull(session.getEntry("3"));

    }

}
