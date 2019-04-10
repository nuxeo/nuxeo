/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
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
        folder1 = session.createDocument(folder1);

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
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "something");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file1.setPropertyValue("dc:creator", "pete");
        file2.setPropertyValue("dc:contributors",
                new String[] { "bob", "pete" });
        file2.setPropertyValue("dc:coverage", "football");
        file2 = session.createDocument(file2);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1",
                "testfile3", "Note");
        file3.setPropertyValue("note", "this is a note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors",
                new String[] { "bob", "john" });
        file3 = session.createDocument(file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2",
                "Folder");
        folder2.setPropertyValue("dc:title", "testfolder2_Title");
        folder2 = session.createDocument(folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2",
                "testfolder3", "Folder");
        folder3.setPropertyValue("dc:title", "testfolder3_Title");
        folder3 = session.createDocument(folder3);

        DocumentModel folder4 = new DocumentModelImpl("/testfolder2",
                "testfolder4", "Folder");
        folder4.setPropertyValue("dc:title", "testfolder4_Title");
        folder4 = session.createDocument(folder4);

        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3",
                "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "something");
        file4 = session.createDocument(file4);

        DocumentModel file5 = new DocumentModelImpl("/testfolder1",
                "testfile5", "File");
        file5.setPropertyValue("dc:title", "title5");
        file5 = session.createDocument(file5);
        file5.followTransition(DELETE_TRANSITION);
        session.saveDocument(file5);
        info.put("file5id", file5.getId());

        session.save();

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();

        return info;
    }

}
