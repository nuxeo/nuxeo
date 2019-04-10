/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.tests;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.chemistry.Repository;
import org.apache.chemistry.RepositoryManager;
import org.apache.chemistry.RepositoryService;
import org.apache.chemistry.impl.simple.SimpleRepositoryService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Test class that runs a servlet on a repository initialized with a few simple
 * documents.
 *
 * @author Florent Guillaume
 */
public class MainServlet extends SQLRepositoryTestCase {

    public static final String REPOSITORY_NAME = "test";

    public static void main(String[] args) throws Exception {
        RepositoryService repositoryService = null;
        MainServlet main = new MainServlet("test");
        main.setUp();
        try {
            repositoryService = new SimpleRepositoryService(
                    main.makeRepository());
            RepositoryManager.getInstance().registerService(repositoryService);
            // if (args.length == 0) {
            // args = new String[] { "-p", "8082" };
            // }
            new org.apache.chemistry.test.MainServlet().run(args, "/cmis",
                    "/repository");
        } finally {
            main.tearDown();
            if (repositoryService != null) {
                RepositoryManager.getInstance().unregisterService(
                        repositoryService);
                repositoryService = null;
            }
        }
    }

    protected MainServlet(String name) {
        super(name);
    }

    // needed to change the repo config
    @Override
    public void deployContrib(String bundle, String contrib) throws Exception {
        if (bundle.equals("org.nuxeo.ecm.core.storage.sql.test")
                && contrib.startsWith("OSGI-INF/test-repo-repository")) {
            bundle = "org.nuxeo.ecm.core.chemistry.tests";
        }
        super.deployContrib(bundle, contrib);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deployBundle("org.nuxeo.ecm.core.event");
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected static Calendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        return cal;
    }

    public Repository makeRepository() throws IOException, ClientException {
        openSession();

        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = "Some caf\u00e9 in a restaurant.\nDrink!.\n";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        file1.setPropertyValue("filename", filename);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "football");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3.setPropertyValue("note:note", "this is a note.\n");
        file3.setPropertyValue("note:mime_type", "text/plain");
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3 = session.createDocument(folder3);

        // create file 4
        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "testfile4_DESCRIPTION4");
        file4 = session.createDocument(file4);

        session.save();
        closeSession();
        return new NuxeoRepository(REPOSITORY_NAME);
    }

}
