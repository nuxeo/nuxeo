/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.api.ws;


/**
 * Nuxeo EP remoting API.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface NuxeoRemoting extends BaseNuxeoWebService {

    /**
     * Gets the current repository name.
     *
     * @param sid the session id
     * @return the repository name
     */
    String getRepositoryName(String sid);

    /**
     * Gets the root document descriptor.
     *
     * @return the root document
     */
    DocumentDescriptor getRootDocument(String sessionId);

    /**
     * Gets the doc descriptor given the doc UUID.
     *
     * @param sessionId the session id
     * @param uuid the doc uuid
     * @return the descriptor
     */
    DocumentDescriptor getDocument(String sessionId, String uuid);

    /**
     * Gets the children of the given document.
     *
     * @param sessionId the session id
     * @param uuid the doc uuid
     * @return the children descriptors
     */
    DocumentDescriptor[] getChildren(String sessionId, String uuid);

    /**
     * Returns the relative path as a displayable path with parent titles.
     * <p>
     * Example: <i>/Workspaces/My Workspaces/Nice Document</i>
     *
     * @param sessionId : the session id
     * @param uuid : the document uuid
     * @return a relative path
     */
    String getRelativePathAsString(String sessionId, String uuid);

    /**
     * Gets the versions of the given document.
     *
     * @param sid
     * @param uid
     * @return
     */
    DocumentDescriptor[] getVersions(String sid, String uid);

    /**
     * Gets the current version of the given document.
     *
     * @param sid
     * @return
     */
    DocumentDescriptor getCurrentVersion(String sid, String uid);

    /**
     * Gets the document that created the version specified by the given uid.
     *
     * @param sid
     * @param uid
     * @return
     */
    DocumentDescriptor getSourceDocument(String sid, String uid);

    /**
     * Returns the document properties.
     * <p>
     * All property are returned even blobs. All values are converted to strings.
     * <p>
     * It includes the perm link of the document.
     * <p>
     * No need to includes blobs here. See the dedicated API.
     *
     * @param uuid uuid of the document.
     * @return a map from name of the property to its value
     */
    DocumentProperty[] getDocumentProperties(String sid, String uuid);

    /**
     * Same as {@link #getDocumentProperties(String, String)} but skips blobs.
     *
     * @param sid
     * @param uuid
     * @return
     */
    DocumentProperty[] getDocumentNoBlobProperties(String sid, String uuid);

    /**
     * Returns the document blobs only using byte[] format
     *
     * @param uuid the uuid of the document.
     * @return an array of document blob instances.
     */
    DocumentBlob[] getDocumentBlobs(String sid, String uuid);

    /**
     * Returns the document blobs only.
     *
     * @param uuid the uuid of the document.
     * @param useDownloadUrl defines if blob are exported as download url or as byte|[]
     * @return an array of document blob instances.
     */
    DocumentBlob[] getDocumentBlobsExt(String sid, String uuid, boolean useDownloadUrl);

    /**
     * Returns the merged ACL of the document (contains all ACEs defined on the document and its parents).
     * <p>
     * It includes all ACLs for each ACP.
     *
     * @param uuid the uuid of the document
     * @return the ordered list of ACLs
     */
    WsACE[] getDocumentACL(String sid, String uuid);

    /**
     * Returns the merged ACL of the document (contains all ACEs defined on the document, filtering the inherited ones).
     * <p>
     * It includes all ACLs for each ACP.
     *
     * @param uuid the uuid of the document
     * @return the ordered list of ACLs
     */
    WsACE[] getDocumentLocalACL(String sid, String uuid);

    /**
     * Checks the given permission for the current user on the given document.
     *
     * @param sid
     * @param uuid
     * @param permission
     * @return
     */
    boolean hasPermission(String sid, String uuid, String permission);

    /**
     * Returns the list of all users.
     * <p>
     * This method supports pagination in case of large user dbs.
     * <p>
     * <b>Pagination is not yet working!</b>
     *
     * @param from pagination start
     * @param to pagination stop
     * @return an array of principal names
     */
    String[] listUsers(String sid, int from, int to);

    /**
     * Return the list of all groups.
     * <p>
     * This method supports pagination in case of large user dbs
     * <p>
     * <b>Pagination is not yet working!</b>
     *
     * @param from pagination start
     * @param to pagination stop
     * @return an array of group names
     */
    String[] listGroups(String sid, int from, int to);

    /**
     * Get all users inside the given group. If the group is null then return all users in the system.
     *
     * @param sid the session id
     * @param parentGroup the parent group
     */
    String[] getUsers(String sid, String parentGroup);

    /**
     * Gets all sub-groups inside the given group. If the parent group is null returns all top level groups.
     *
     * @param sid the session id
     * @param parentGroup the parent group
     * @return
     */
    String[] getGroups(String sid, String parentGroup);

    String uploadDocument(String sid, String path, String type, String[] properties);

    /**
     * Gets all properties and ACLs from a document uses byte[] format to export blob
     *
     * @param sid the session id
     * @param uuid the doc uuid
     * @return
     */
    DocumentSnapshot getDocumentSnapshot(String sid, String uuid);

    /**
     * Gets all properties and ACLs from a document
     *
     * @param sid the session id
     * @param uuid the doc uuid
     * @param useDownloadUrl define blob export format
     * @return
     */
    DocumentSnapshot getDocumentSnapshotExt(String sid, String uuid, boolean useDownloadUrl);
}
