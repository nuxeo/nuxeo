/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Various static methods used in several test cases.
 */
public class Helper {

    public static final String DELETE_TRANSITION = "delete";

    public static final String FILE1_CONTENT = "Noodles with rice";

    /**
     * Reads a stream into a string.
     */
    public static String read(InputStream in, String charset)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return os.toString(charset);
    }

    /**
     * Gets a Calendar object.
     */
    public static GregorianCalendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds) {
        TimeZone tz = TimeZone.getTimeZone("GMT-02:00"); // in the Atlantic
        return getCalendar(year, month, day, hours, minutes, seconds, tz);
    }

    /**
     * Gets a Calendar object with a specific timezone
     */
    public static GregorianCalendar getCalendar(int year, int month, int day,
            int hours, int minutes, int seconds, TimeZone tz) {
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(tz);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1); // 0-based
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Creates data in the repository using the Nuxeo API. This is then used as
     * a starting point by unit tests.
     */
    public static Map<String, String> makeNuxeoRepository(CoreSession session)
            throws Exception {
        Map<String, String> info = new HashMap<String, String>();

        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = createDocument(session, folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = FILE1_CONTENT;
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        blob1.setFilename(filename);
        file1.setPropertyValue("content", blob1);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0);
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:modified", cal1);
        file1.setPropertyValue("dc:creator", "michael");
        file1.setPropertyValue("dc:lastContributor", "john");
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1 = createDocument(session, file1);

        ACPImpl acp;
        ACL acl;
        acl = new ACLImpl();
        acl.add(new ACE("bob", SecurityConstants.BROWSE, true));
        acp = new ACPImpl();
        acp.addACL(acl);
        file1.setACP(acp, true);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "something");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:creator", "pete");
        file2.setPropertyValue("dc:contributors",
                new String[] { "pete", "bob" });
        file2.setPropertyValue("dc:lastContributor", "bob");
        file2.setPropertyValue("dc:coverage", "football");
        file2 = createDocument(session, file2);

        acl = new ACLImpl();
        acl.add(new ACE("bob", SecurityConstants.BROWSE, true));
        acp = new ACPImpl();
        acp.addACL(acl);
        file2.setACP(acp, true);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("note", "this is a note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3.setPropertyValue("dc:lastContributor", "john");
        file3 = createDocument(session, file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2.setPropertyValue("dc:title", "testfolder2_Title");
        folder2 = createDocument(session, folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3.setPropertyValue("dc:title", "testfolder3_Title");
        folder3 = createDocument(session, folder3);

        DocumentModel folder4 = new DocumentModelImpl("/testfolder2",
                "testfolder4", "Folder");
        folder4.setPropertyValue("dc:title", "testfolder4_Title");
        folder4 = createDocument(session, folder4);

        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "something");
        file4 = createDocument(session, file4);

        DocumentModel file5 = new DocumentModelImpl("/testfolder1",
                "testfile5", "File");
        file5.setPropertyValue("dc:title", "title5");
        file5 = createDocument(session, file5);
        file5.followTransition(DELETE_TRANSITION);
        file5 = saveDocument(session, file5);
        info.put("file5id", file5.getId());

        session.save();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();

        return info;
    }

    /**
     * For audit, make sure event dates don't have the same millisecond.
     */
    public static void sleepForAuditGranularity() throws InterruptedException {
        Thread.sleep(2);
    }

    public static DocumentModel createDocument(CoreSession session,
            DocumentModel doc) throws Exception {
        sleepForAuditGranularity();
        return session.createDocument(doc);
    }

    public static DocumentModel saveDocument(CoreSession session,
            DocumentModel doc) throws Exception {
        sleepForAuditGranularity();
        return session.saveDocument(doc);
    }

    public static String createUserWorkspace(CoreSession repo, String username) throws ClientException {

        DocumentModel container = new DocumentModelImpl("/", "UserWorkspaceRoot", "UserWorkspaceRoot");
        container = repo.createDocument(container);
        {
            ACP acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[]{new ACE(SecurityConstants.EVERYONE,
                    SecurityConstants.EVERYTHING, false)});
            acp.addACL(acl);
            container.setACP(acp, true);
        }

        DocumentModel ws = new DocumentModelImpl(container.getPathAsString(), username, "Workspace");
        ws = repo.createDocument(ws);
        ACP acp = new ACPImpl();
        {
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[]{new ACE(username, SecurityConstants.EVERYTHING, true)});
            acp.addACL(acl);
            ws.setACP(acp, true);
        }

        repo.save();
        return ws.getPathAsString();
    }
}
