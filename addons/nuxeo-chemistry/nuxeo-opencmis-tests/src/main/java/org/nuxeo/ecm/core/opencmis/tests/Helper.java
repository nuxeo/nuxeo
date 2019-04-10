/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Various static methods used in several test cases.
 */
public class Helper {

    public static final String DELETE_TRANSITION = "delete";

    public static final String FILE1_CONTENT = "Noodles with rice";

    /**
     * Reads a stream into a string.
     */
    public static String read(InputStream in, String charset) throws IOException {
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
    public static GregorianCalendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds) {
        TimeZone tz = TimeZone.getTimeZone("GMT-02:00"); // in the Atlantic
        return getCalendar(year, month, day, hours, minutes, seconds, tz);
    }

    /**
     * Gets a Calendar object with a specific timezone
     */
    public static GregorianCalendar getCalendar(int year, int month, int day, int hours, int minutes, int seconds,
            TimeZone tz) {
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

    public static Map<String, String> makeNuxeoRepository(CoreSession session) {
        return makeNuxeoRepository(session, false);
    }

    /**
     * Creates data in the repository using the Nuxeo API. This is then used as a starting point by unit tests.
     */
    public static Map<String, String> makeNuxeoRepository(CoreSession session, boolean addProxy) {
        // remove default-domain
        DocumentRef defaultDomain = new PathRef("/default-domain");
        if (session.exists(defaultDomain)) {
            session.removeDocument(defaultDomain);
        }

        Map<String, String> info = new HashMap<String, String>();

        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1", "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = createDocument(session, folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1", "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = FILE1_CONTENT;
        String filename = "testfile.txt";
        Blob blob1 = Blobs.createBlob(content);
        blob1.setDigest(DigestUtils.md5Hex(content));
        blob1.setFilename(filename);
        file1.setPropertyValue("content", (Serializable) blob1);
        Calendar cal1 = getCalendar(2007, 3, 1, 12, 0, 0, TimeZone.getDefault());
        file1.setPropertyValue("dc:created", cal1);
        file1.setPropertyValue("dc:modified", cal1);
        file1.setPropertyValue("dc:creator", "michael");
        file1.setPropertyValue("dc:lastContributor", "john");
        file1.setPropertyValue("dc:coverage", "foo/bar");
        file1.setPropertyValue("dc:subjects", new String[] { "foo", "gee/moo" });
        file1.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
        Blob thumbnailBlob;
        String digest;
        try {
            thumbnailBlob = Blobs.createBlob(Helper.class.getResource("/text.png").openStream(), "image/png");
            try (InputStream stream = thumbnailBlob.getStream()) {
            	digest = DigestUtils.md5Hex(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        thumbnailBlob.setDigest(digest);
        thumbnailBlob.setFilename("test.png");
        file1.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, (Serializable) thumbnailBlob);
        file1 = createDocument(session, file1);

        ACPImpl acp;
        ACL acl;
        acl = new ACLImpl();
        acl.add(new ACE("bob", SecurityConstants.BROWSE, true));
        acp = new ACPImpl();
        acp.addACL(acl);
        file1.setACP(acp, true);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1", "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "something");
        Calendar cal2 = getCalendar(2007, 4, 1, 12, 0, 0);
        file2.setPropertyValue("dc:created", cal2);
        file2.setPropertyValue("dc:creator", "pete");
        file2.setPropertyValue("dc:contributors", new String[] { "pete", "bob" });
        file2.setPropertyValue("dc:lastContributor", "bob");
        file2.setPropertyValue("dc:coverage", "football");
        file2 = createDocument(session, file2);

        acl = new ACLImpl();
        acl.add(new ACE("bob", SecurityConstants.BROWSE, true));
        acp = new ACPImpl();
        acp.addACL(acl);
        file2.setACP(acp, true);

        DocumentModel file3 = new DocumentModelImpl("/testfolder1", "testfile3", "Note");
        file3.setPropertyValue("note", "this is a note");
        file3.setPropertyValue("dc:title", "testfile3_Title");
        file3.setPropertyValue("dc:description", "testfile3_desc1 testfile3_desc2,  testfile3_desc3");
        file3.setPropertyValue("dc:contributors", new String[] { "bob", "john" });
        file3.setPropertyValue("dc:lastContributor", "john");
        file3 = createDocument(session, file3);

        DocumentModel folder2 = new DocumentModelImpl("/", "testfolder2", "Folder");
        folder2.setPropertyValue("dc:title", "testfolder2_Title");
        folder2 = createDocument(session, folder2);

        DocumentModel folder3 = new DocumentModelImpl("/testfolder2", "testfolder3", "Folder");
        folder3.setPropertyValue("dc:title", "testfolder3_Title");
        folder3 = createDocument(session, folder3);

        DocumentModel folder4 = new DocumentModelImpl("/testfolder2", "testfolder4", "Folder");
        folder4.setPropertyValue("dc:title", "testfolder4_Title");
        folder4 = createDocument(session, folder4);

        DocumentModel file4 = new DocumentModelImpl("/testfolder2/testfolder3", "testfile4", "File");
        file4.setPropertyValue("dc:title", "testfile4_Title");
        file4.setPropertyValue("dc:description", "something");
        file4 = createDocument(session, file4);

        DocumentModel file5 = new DocumentModelImpl("/testfolder1", "testfile5", "File");
        file5.setPropertyValue("dc:title", "title5");
        file5 = createDocument(session, file5);
        file5.followTransition(DELETE_TRANSITION);
        file5 = saveDocument(session, file5);
        info.put("file5id", file5.getId());

        DocumentModel file6 = new DocumentModelImpl("/testfolder2/testfolder3", "testfile6", "Note");
        file6.setPropertyValue("dc:title", "title6");
        file6 = createDocument(session, file6);

        if (addProxy) {
            sleepForAuditGranularity();
            file6.putContextData("disableDublinCoreListener", Boolean.TRUE);
            DocumentRef file6verref = file6.checkIn(VersioningOption.MINOR, null);

            sleepForAuditGranularity();
            DocumentModel proxy = session.createProxy(file6verref, folder2.getRef());
            info.put("file6verid", proxy.getSourceId());
            info.put("proxyid", proxy.getId());
        }

        return info;
    }

    /**
     * For audit, make sure event dates don't have the same millisecond.
     */
    public static void sleepForAuditGranularity() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            ExceptionUtils.checkInterrupt(e);
        }
    }

    public static DocumentModel createDocument(CoreSession session, DocumentModel doc) {
        sleepForAuditGranularity();
        // avoid changes in last contributor in these tests
        doc.putContextData("disableDublinCoreListener", Boolean.TRUE);
        return session.createDocument(doc);
    }

    public static DocumentModel saveDocument(CoreSession session, DocumentModel doc) {
        sleepForAuditGranularity();
        return session.saveDocument(doc);
    }

    public static String createUserWorkspace(CoreSession repo, String username) {

        DocumentModel container = new DocumentModelImpl("/", "UserWorkspaceRoot", "UserWorkspaceRoot");
        container = repo.createDocument(container);
        {
            ACP acp = new ACPImpl();
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[] { new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false) });
            acp.addACL(acl);
            container.setACP(acp, true);
        }

        DocumentModel ws = new DocumentModelImpl(container.getPathAsString(), username, "Workspace");
        ws = repo.createDocument(ws);
        ACP acp = new ACPImpl();
        {
            ACL acl = new ACLImpl();
            acl.setACEs(new ACE[] { new ACE(username, SecurityConstants.EVERYTHING, true) });
            acp.addACL(acl);
            ws.setACP(acp, true);
        }

        repo.save();
        if (TransactionHelper.isTransactionActive()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        return ws.getPathAsString();
    }
}
