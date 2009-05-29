package org.nuxeo.ecm.platform.importer.tests;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.executor.DefaultImporterExecutor;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class SimpleImporterTest extends SQLRepositoryTestCase {


    public SimpleImporterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        openSession();
    }

    /*
    public void testImport() throws Exception {

        String inputPath = "/home/tiry/docs/tests/";
        String targetPath = "/default-domain/workspaces/";

        DefaultImporterExecutor executor = new DefaultImporterExecutor(session);

        executor.run(inputPath, targetPath, 5, 5, true);

        long createdDocs = executor.getCreatedDocsCounter();
        assertTrue(createdDocs > 0);
    }*/


    public void testRamdomImport() throws Exception {

        System.out.println("Starting prefil");
        SourceNode src = RandomTextSourceNode.init(2000);
        System.out.println("prefil done");
        String targetPath = "/default-domain/workspaces/";

        DefaultImporterExecutor executor = new DefaultImporterExecutor(session);

        executor.run(src, targetPath, 10, 5, true);

        long createdDocs = executor.getCreatedDocsCounter();
        assertTrue(createdDocs > 0);

        System.out.println("total content size = " + RandomTextSourceNode.getSize()/(1024*1024) + " MB");
        System.out.println("nb nodes = " + RandomTextSourceNode.getNbNodes());
    }


}
