package org.nuxeo.ecm.platform.template.tests;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.template.processors.xdocreport.FieldDefinitionGenerator;

public class TestFieldDefinitionGeneration extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        openSession();
    }


    public void testGeneration() throws Exception {        
        FieldDefinitionGenerator.generate(session.getRootDocument());
    }
    
    @Override
    public void tearDown() {
        closeSession();
    }


}
