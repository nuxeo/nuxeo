/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * Default repository initializer that create the default DM doc hierarchy.
 */
public class CoreDirectoryInit implements RepositoryInit {

    public static String ROOT_FOLDER_PATH = "/rootFolder";

    public static String DOC_ID_USER1 = "user1";

    public static String DOC_PWD_USER1 = "foo1";

    public static String DOC_ID_USER2 = "user2";

    public static String DOC_PWD_USER2 = "foo2";

    public static String DOC_ID_USERSHA1 = "usersha1";

    public static String DOC_PWD_USERSHA1 = "foosha1";

    // one encrypted password value of clear text password DOC_PWD_USERSHA1 = "foosha1"
    public static String ENC_PWD_USERSHA1 = "{SSHA}wbm44K3IQWduZj8U+kl0dvAkXvFbxuSe1WmxmQ==";

    public static String DOC_PWD_BADPWDSHA1 = "bad-pwd";

    public static String USERS_RESTRICTED_FOLDER = "users-restricted";

    public static String USERS_UNRESTRICTED_FOLDER = "users-unrestricted";

    public static String USERS_RESTRICTED_PATH = ROOT_FOLDER_PATH + "/test/" + USERS_RESTRICTED_FOLDER;

    public static String USERS_UNRESTRICTED_PATH = ROOT_FOLDER_PATH + "/test/" + USERS_UNRESTRICTED_FOLDER;

    @Override
    public void populate(CoreSession session) {
        // create root folder manually, like would be done in CoreDirectory.start
        DocumentModel rootFolder = session.createDocumentModel("/", ROOT_FOLDER_PATH.substring(1), "Workspace");
        rootFolder = session.createDocument(rootFolder);
        // add write ACL for user_1
        ACP acp = rootFolder.getACP();
        ACL localACL = acp.getOrCreateACL();
        localACL.add(new ACE(CoreDirectoryFeature.USER1_NAME, SecurityConstants.WRITE, true));
        session.setACP(rootFolder.getRef(), acp, true);

        DocumentModel doc = session.createDocumentModel(ROOT_FOLDER_PATH, "test", "Workspace");
        doc.setProperty("dublincore", "title", "test");
        doc = session.createDocument(doc);

        doc = createDocument(session, ROOT_FOLDER_PATH + "/test", USERS_RESTRICTED_FOLDER, "Folder");

        // user_2 has no permission on it

        acp = doc.getACP();
        localACL = doc.getACP().getOrCreateACL("local");
        localACL.clear();
        localACL.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        localACL.add(new ACE(CoreDirectoryFeature.USER1_NAME, SecurityConstants.EVERYTHING, true));
        localACL.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        session.setACP(doc.getRef(), acp, true);

        // Create a User1 doc for unit test
        DocumentModel user1 = createDocument(session, doc.getPathAsString(), "User1", "CoreDirDoc");
        user1.setProperty("schema1", "uid", DOC_ID_USER1);
        user1.setProperty("schema1", "foo", DOC_PWD_USER1);
        user1.setProperty("schema1", "bar", "bar1");
        session.saveDocument(user1);

        // Creates SHA1 passwords for unit test
        DocumentModel userSHA1 = createDocument(session, doc.getPathAsString(), "UserSHA1", "CoreDirDoc");
        userSHA1.setProperty("schema1", "uid", DOC_ID_USERSHA1);
        userSHA1.setProperty("schema1", "foo", ENC_PWD_USERSHA1);
        userSHA1.setProperty("schema1", "bar", "barsha1");
        session.saveDocument(userSHA1);

        doc = createDocument(session, ROOT_FOLDER_PATH + "/test", USERS_UNRESTRICTED_FOLDER, "Folder");

        acp = doc.getACP();
        localACL = doc.getACP().getOrCreateACL("local");
        localACL.clear();
        localACL.add(new ACE("Administrator", SecurityConstants.EVERYTHING, true));
        localACL.add(new ACE(CoreDirectoryFeature.USER1_NAME, SecurityConstants.EVERYTHING, true));
        localACL.add(new ACE(CoreDirectoryFeature.USER2_NAME, SecurityConstants.EVERYTHING, true));
        localACL.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        session.setACP(doc.getRef(), acp, true);

        // Create a User2 doc for unit test
        DocumentModel user2 = createDocument(session, doc.getPathAsString(), "User2", "CoreDirDoc");
        user2.setProperty("schema1", "uid", DOC_ID_USER2);
        user2.setProperty("schema1", "foo", DOC_PWD_USER2);
        user2.setProperty("schema1", "bar", "bar2");
        session.saveDocument(user2);

    }

    public DocumentModel createDocument(CoreSession session, String parentPath, String docName, String docType) {
        DocumentModel doc = session.createDocumentModel(parentPath, docName, docType);
        doc.setProperty("dublincore", "title", docType);
        return session.createDocument(doc);
    }

    public DocumentModel createDomain(CoreSession session, String domainName, String domainTitle) {
        DocumentModel doc = session.createDocumentModel("/", domainName, "Domain");
        doc.setProperty("dublincore", "title", domainTitle);
        doc = session.createDocument(doc);
        DocumentModel docDomain = doc;

        doc = session.createDocumentModel("/" + domainName + "/", "workspaces", "WorkspaceRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/" + domainName + "/", "sections", "SectionRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        doc = session.createDocument(doc);

        doc = session.createDocumentModel("/" + domainName + "/", "templates", "TemplateRoot");
        doc.setProperty("dublincore", "title", "Templates");
        doc.setProperty("dublincore", "description", "Root of workspaces templates");
        doc = session.createDocument(doc);

        return docDomain;
    }
}
