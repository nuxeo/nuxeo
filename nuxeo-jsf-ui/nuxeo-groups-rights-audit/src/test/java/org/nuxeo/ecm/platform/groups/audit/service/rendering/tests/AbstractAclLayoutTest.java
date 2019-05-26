/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */
package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.google.common.collect.Lists;

public class AbstractAclLayoutTest {
    protected static String folder = "target/";

    protected DocumentModel makeFolder(CoreSession session, String path, String name, boolean save)
            throws PropertyException {
        return makeItem(session, path, name, "Folder", save);
    }

    protected DocumentModel makeFolder(CoreSession session, String path, String name) throws PropertyException {
        return makeItem(session, path, name, "Folder", true);
    }

    protected DocumentModel makeDoc(CoreSession session, String path, String name) throws PropertyException {
        return makeItem(session, path, name, "Document", true);
    }

    protected DocumentModel makeItem(CoreSession session, String path, String name, String type, boolean save)
            throws PropertyException {
        DocumentModel folder = session.createDocumentModel(path, name, type);
        folder = session.createDocument(folder);
        if (save) {
            session.saveDocument(folder);
        }
        return folder;
    }

    protected DocumentModel makeGroup(UserManager userManager, String groupId) throws Exception {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    protected DocumentModel makeUser(UserManager userManager, String userId) throws Exception {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }

    protected void addAcl(CoreSession session, DocumentModel doc, String userOrGroup, String right, boolean allow) {
        addAcl(session, doc, userOrGroup, right, allow, false);
    }

    protected void addAcl(CoreSession session, DocumentModel doc, String userOrGroup, String right, boolean allow,
            boolean blockInheritance) {
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();// local
        acl.add(new ACE(userOrGroup, right, allow));
        doc.setACP(acp, true);
        session.saveDocument(doc);
    }

    protected void addAclLockInheritance(CoreSession session, DocumentModel doc, String userOrGroup, boolean save) {
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();// local
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        doc.setACP(acp, true);
        if (save) {
            session.saveDocument(doc);
        }
    }

    /* BUILD DOC TREE */

    /**
     * Do not forget to call session.save() d=2 w=10 > 11 d=3 w=10 > 111 d=5 w=10 > 11111 ~15sec to generate d=6 w=10 >
     * ~1 min to generate
     */
    protected DocumentModel makeDocumentTree(CoreSession session, int depth, int width, int nGroups) throws Exception {
        DocumentModel root = makeFolder(session, "/", "root", false);

        List<String> groups = new ArrayList<>(nGroups);
        for (int i = 0; i < nGroups; i++) {
            String group = "group" + i;
            groups.add(group);
        }

        return makeDocumentTree(session, depth, width, nGroups, 1, root, groups);
    }

    protected int MOD = 3;

    protected DocumentModel makeDocumentTree(CoreSession session, int maxDepth, int width, int nGroups,
            int currentDepth, DocumentModel folder, List<String> groups) throws PropertyException {

        // populate current folder with ACL
        boolean save = false;
        for (String group : groups) {
            addAcl(session, folder, group, SecurityConstants.READ_WRITE, true, save);
            addAcl(session, folder, group, SecurityConstants.ADD_CHILDREN, true, save);
            addAcl(session, folder, group, SecurityConstants.REMOVE_CHILDREN, true, save);
            addAcl(session, folder, group, SecurityConstants.READ_LIFE_CYCLE, true, save);
            addAcl(session, folder, group, SecurityConstants.WRITE_SECURITY, true, save);

            // final rule with lock inherit
            if (currentDepth != 0 && currentDepth % MOD == 0) {
                addAclLockInheritance(session, folder, group, save);
            }
        }

        // generate children folders
        if (currentDepth < maxDepth) {
            // dispatch all groups into each tree branch
            List<List<String>> subgroups;

            int subgroupSize = groups.size() / width;
            if (subgroupSize > 0) { // general case
                subgroups = Lists.partition(groups, subgroupSize);
            } else {
                if (groups.size() >= 2) {
                    subgroups = Lists.partition(groups, 1);
                } else {
                    subgroups = new ArrayList<>();
                    subgroups.add(groups);
                }
            }

            for (int i = 0; i < width; i++) {
                // create a folder
                String name = "[" + currentDepth + "]folder-" + i;
                DocumentModel f = makeFolder(session, folder.getPathAsString(), name);

                // generate children folders
                if (i < subgroups.size()) {
                    List<String> subgroup = subgroups.get(i);
                    makeDocumentTree(session, maxDepth, width, nGroups, currentDepth + 1, f, subgroup);
                } else {
                    makeDocumentTree(session, maxDepth, width, nGroups, currentDepth + 1, f, new ArrayList<>());
                }
            }
            return folder;
        }
        // or end recursion
        else {
            return folder;
        }
    }

    /* */

    protected static String get(Workbook w, int s, int r, int c) {
        Sheet sheet = w.getSheetAt(s);
        if (sheet != null) {
            Row row = sheet.getRow(r);
            if (row != null) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    RichTextString rts = cell.getRichStringCellValue();
                    return rts.getString();
                } else {
                    fail("no cell at id " + c + " in row " + r + " at sheet " + s);
                }
            } else {
                fail("no row at id " + r + " in sheet " + s);
            }
        } else {
            fail("no sheet at id " + s);
        }
        return null;
    }

}
