/*
 * (C) Copyright 2007-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: NuxeoRemotingBean.java 13219 2007-03-03 18:43:31Z bstefanescu $
 */

package org.nuxeo.ecm.platform.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.api.ws.DocumentBlob;
import org.nuxeo.ecm.platform.api.ws.DocumentDescriptor;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.DocumentSnapshot;
import org.nuxeo.ecm.platform.api.ws.NuxeoRemoting;
import org.nuxeo.ecm.platform.api.ws.WsACE;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo remoting stateful session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@WebService(name = "NuxeoRemotingInterface", serviceName = "NuxeoRemotingService")
@SOAPBinding(style = Style.DOCUMENT)
public class NuxeoRemotingBean extends AbstractNuxeoWebService implements NuxeoRemoting {

    private static final long serialVersionUID = 359922583442116202L;

    private static final Log log = LogFactory.getLog(NuxeoRemotingBean.class);

    @Override
    @WebMethod
    public String getRepositoryName(@WebParam(name = "sessionId") String sid) {
        WSRemotingSession rs = initSession(sid);
        return rs.getRepository();
    }

    @Override
    @WebMethod
    public WsACE[] getDocumentACL(@WebParam(name = "sessionId") String sid, @WebParam(name = "uuid") String uuid)
            {
        WSRemotingSession rs = initSession(sid);
        ACP acp = rs.getDocumentManager().getACP(new IdRef(uuid));
        if (acp != null) {
            ACL acl = acp.getMergedACLs("MergedACL");
            return WsACE.wrap(acl.toArray(new ACE[acl.size()]));
        } else {
            return null;
        }
    }

    @Override
    @WebMethod
    public DocumentSnapshot getDocumentSnapshot(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid) {
        return getDocumentSnapshotExt(sid, uuid, false);
    }

    @Override
    public DocumentSnapshot getDocumentSnapshotExt(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid, @WebParam(name = "useDownloadURL") boolean useDownloadUrl)
            {
        WSRemotingSession rs = initSession(sid);
        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));

        DocumentProperty[] props = getDocumentNoBlobProperties(doc, rs);
        DocumentBlob[] blobs = getDocumentBlobs(doc, rs, useDownloadUrl);

        ACE[] resACP = null;

        ACP acp = doc.getACP();
        if (acp != null) {
            ACL acl = acp.getMergedACLs("MergedACL");
            resACP = acl.toArray(new ACE[acl.size()]);
        }
        DocumentSnapshot ds = new DocumentSnapshot(props, blobs, doc.getPathAsString(), WsACE.wrap(resACP));
        return ds;
    }

    @Override
    @WebMethod
    public WsACE[] getDocumentLocalACL(@WebParam(name = "sessionId") String sid, @WebParam(name = "uuid") String uuid)
            {
        WSRemotingSession rs = initSession(sid);
        ACP acp = rs.getDocumentManager().getACP(new IdRef(uuid));
        if (acp != null) {
            ACL mergedAcl = new ACLImpl("MergedACL", true);
            for (ACL acl : acp.getACLs()) {
                if (!ACL.INHERITED_ACL.equals(acl.getName())) {
                    mergedAcl.addAll(acl);
                }
            }
            return WsACE.wrap(mergedAcl.toArray(new ACE[mergedAcl.size()]));
        } else {
            return null;
        }
    }

    @Override
    public boolean hasPermission(@WebParam(name = "sessionId") String sid, @WebParam(name = "uuid") String uuid,
            @WebParam(name = "permission") String permission) {
        WSRemotingSession rs = initSession(sid);
        CoreSession docMgr = rs.getDocumentManager();
        DocumentModel doc = docMgr.getDocument(new IdRef(uuid));
        if (doc == null) {
            throw new DocumentNotFoundException("No such document: " + uuid);
        }
        return docMgr.hasPermission(doc.getRef(), permission);
    }

    @Override
    @WebMethod
    public DocumentBlob[] getDocumentBlobs(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid) {
        return getDocumentBlobsExt(sid, uuid, false);
    }

    @Override
    public DocumentBlob[] getDocumentBlobsExt(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid, @WebParam(name = "useDownloadUrl") boolean useDownloadUrl)
            {
        WSRemotingSession rs = initSession(sid);
        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));
        if (doc == null) {
            return null;
        }

        return getDocumentBlobs(doc, rs, useDownloadUrl);
    }

    protected DocumentBlob[] getDocumentBlobs(DocumentModel doc, WSRemotingSession rs, boolean useDownloadUrl)
            {
        List<DocumentBlob> blobs = new ArrayList<DocumentBlob>();
        String[] schemas = doc.getSchemas();
        for (String schema : schemas) {
            DataModel dm = doc.getDataModel(schema);
            Map<String, Object> map = dm.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectBlobs(doc.getId(), schema, rs, "", map, entry.getKey(), entry.getValue(), blobs, useDownloadUrl);
            }
        }
        return blobs.toArray(new DocumentBlob[blobs.size()]);
    }

    @Override
    @WebMethod
    public String[] listUsers(@WebParam(name = "sessionId") String sid, @WebParam(name = "startIndex") int from,
            @WebParam(name = "endIndex") int to) {
        WSRemotingSession rs = initSession(sid);
        List<String> userIds = rs.getUserManager().getUserIds();
        return userIds.toArray(new String[userIds.size()]);
    }

    @Override
    @WebMethod
    public String[] listGroups(@WebParam(name = "sessionId") String sid, @WebParam(name = "startIndex") int from,
            @WebParam(name = "endIndex") int to) {
        WSRemotingSession rs = initSession(sid);
        List<String> groupIds = rs.getUserManager().getGroupIds();
        return groupIds.toArray(new String[groupIds.size()]);
    }

    @Override
    @WebMethod
    public DocumentProperty[] getDocumentProperties(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sid);

        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));
        List<DocumentProperty> props = new ArrayList<DocumentProperty>();
        if (doc != null) {
            String[] schemas = doc.getSchemas();
            for (String schema : schemas) {
                DataModel dm = doc.getDataModel(schema);
                Map<String, Object> map = dm.getMap();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    collectProperty("", entry.getKey(), entry.getValue(), props);
                }
            }
        }
        return props.toArray(new DocumentProperty[props.size()]);
    }

    @Override
    @WebMethod
    public DocumentProperty[] getDocumentNoBlobProperties(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sid);

        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));
        return getDocumentNoBlobProperties(doc, rs);
    }

    protected DocumentProperty[] getDocumentNoBlobProperties(DocumentModel doc, WSRemotingSession rs)
            {

        List<DocumentProperty> props = new ArrayList<DocumentProperty>();
        if (doc != null) {
            DocumentLoader loader = Framework.getLocalService(DocumentLoader.class);
            loader.fillProperties(doc, props, rs);
        }
        return props.toArray(new DocumentProperty[props.size()]);
    }

    @Override
    public DocumentDescriptor getCurrentVersion(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sid);
        DocumentModel doc = rs.getDocumentManager().getLastDocumentVersion(new IdRef(uuid));
        if (doc != null) {
            return new DocumentDescriptor(doc, doc.getVersionLabel());
        }
        return null;
    }

    @Override
    public DocumentDescriptor getSourceDocument(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uid) {
        WSRemotingSession rs = initSession(sid);
        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uid));
        String srcid = doc.getSourceId();
        if (srcid != null && !srcid.equals(uid)) {
            doc = rs.getDocumentManager().getSourceDocument(doc.getRef());
        }
        if (doc != null) {
            return new DocumentDescriptor(doc);
        }
        return null;
    }

    @Override
    public DocumentDescriptor[] getVersions(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "uuid") String uid) {
        WSRemotingSession rs = initSession(sid);
        List<DocumentModel> versions = rs.getDocumentManager().getVersions(new IdRef(uid));
        if (versions == null) {
            return null;
        }
        DocumentDescriptor[] docs = new DocumentDescriptor[versions.size()];
        int i = 0;
        for (DocumentModel version : versions) {
            docs[i++] = new DocumentDescriptor(version, version.getVersionLabel());
        }
        return null;
    }

    @Override
    @WebMethod
    public DocumentDescriptor getRootDocument(@WebParam(name = "sessionId") String sessionId) {
        WSRemotingSession rs = initSession(sessionId);
        DocumentModel doc = rs.getDocumentManager().getRootDocument();
        return doc != null ? new DocumentDescriptor(doc) : null;
    }

    @Override
    @WebMethod
    public DocumentDescriptor getDocument(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sessionId);
        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));
        return doc != null ? new DocumentDescriptor(doc) : null;
    }

    @Override
    @WebMethod
    public DocumentDescriptor[] getChildren(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sessionId);
        DocumentModelList docList = rs.getDocumentManager().getChildren(new IdRef(uuid));
        DocumentDescriptor[] docs = new DocumentDescriptor[docList.size()];
        int i = 0;
        for (DocumentModel doc : docList) {
            docs[i++] = new DocumentDescriptor(doc);
        }
        return docs;
    }

    @SuppressWarnings("unchecked")
    protected void collectProperty(String prefix, String name, Object value, List<DocumentProperty> props)
            {
        final String STRINGS_LIST_SEP = ";";
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            prefix = prefix + name + '/';
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectProperty(prefix, entry.getKey(), entry.getValue(), props);
            }
        } else if (value instanceof List) {
            prefix = prefix + name + '/';
            List<Object> list = (List<Object>) value;
            for (int i = 0, len = list.size(); i < len; i++) {
                collectProperty(prefix, String.valueOf(i), list.get(i), props);
            }
        } else {
            String strValue = null;
            if (value != null) {
                if (value instanceof Blob) {
                    try {
                        // strValue = ((Blob) value).getString();
                        byte[] bytes = ((Blob) value).getByteArray();
                        strValue = Base64.encodeBase64String(bytes);
                    } catch (IOException e) {
                        throw new NuxeoException("Failed to get blob property value", e);
                    }
                } else if (value instanceof Calendar) {
                    strValue = ((Calendar) value).getTime().toString();
                } else if (value instanceof String[]) {
                    for (String each : (String[]) value) {
                        if (strValue == null) {
                            strValue = each;
                        } else {
                            strValue = strValue + STRINGS_LIST_SEP + each;
                        }
                    }
                    // FIXME: this condition is always false here.
                } else if (value instanceof List) {
                    for (String each : (List<String>) value) {
                        if (strValue == null) {
                            strValue = each;
                        } else {
                            strValue = strValue + STRINGS_LIST_SEP + each;
                        }
                    }
                } else {
                    strValue = value.toString();
                } // TODO: use decode method from field type?
            }
            props.add(new DocumentProperty(prefix + name, strValue));
        }
    }

    @SuppressWarnings("unchecked")
    protected void collectBlobs(String docId, String schemaName, WSRemotingSession rs, String prefix,
            Map<String, Object> container, String name, Object value, List<DocumentBlob> blobs, boolean useDownloadUrl)
            {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            prefix = prefix + name + '/';
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectBlobs(docId, schemaName, rs, prefix, map, entry.getKey(), entry.getValue(), blobs,
                        useDownloadUrl);
            }
        } else if (value instanceof List) {
            prefix = prefix + name + '/';
            List<Object> list = (List<Object>) value;
            for (int i = 0, len = list.size(); i < len; i++) {
                collectBlobs(docId, schemaName, rs, prefix, container, String.valueOf(i), list.get(i), blobs,
                        useDownloadUrl);
            }
        } else if (value instanceof Blob) {
            try {
                Blob blob = (Blob) value;
                String filename = (String) container.get("filename");
                if (filename == null) {
                    filename = prefix + name;
                }

                DocumentBlob db = null;
                if (useDownloadUrl) {
                    String repoName = rs.getDocumentManager().getRepositoryName();
                    String downloadUrl = getDownloadUrl(repoName, docId, schemaName, prefix + name, filename);
                    db = new DocumentBlob(filename, blob.getEncoding(), blob.getMimeType(), downloadUrl);
                } else {
                    db = new DocumentBlob(filename, blob);
                }

                // List<String> extensions =
                // rs.mimeTypeReg.getExtensionsFromMimetypeName(blob.getMimeType());
                // if (extensions != null) {
                // db.setExtensions(extensions.toArray(new
                // String[extensions.size()]));
                // }
                blobs.add(db);
            } catch (IOException e) {
                throw new NuxeoException("Failed to get document blob", e);
            }
        }
    }

    protected String getSchemaPrefix(String schemaName) {
        // XXX : no API to get the prefix from the schemaName !
        return schemaName;
    }

    protected String getDownloadUrl(String repoName, String docId, String schemaName, String xPath, String fileName) {
        String xpath = schemaName + ':' + xPath;
        DownloadService downloadService = Framework.getService(DownloadService.class);
        return "/" + downloadService.getDownloadUrl(repoName, docId, xpath, fileName);
    }

    @Override
    @WebMethod
    public String[] getUsers(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "parentGroup") String parentGroup) {
        if (parentGroup == null) {
            return listUsers(sid, 0, Integer.MAX_VALUE);
        }
        WSRemotingSession rs = initSession(sid);
        NuxeoGroup group = rs.getUserManager().getGroup(parentGroup);
        if (group == null) {
            return null;
        }
        List<String> users = group.getMemberUsers();
        return users.toArray(new String[users.size()]);
    }

    @Override
    @WebMethod
    public String[] getGroups(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "parentGroup") String parentGroup) {
        WSRemotingSession rs = initSession(sid);

        List<String> groups;
        if (parentGroup == null) {
            groups = rs.getUserManager().getTopLevelGroups();
        } else {
            groups = rs.getUserManager().getGroupsInGroup(parentGroup);
        }
        return groups.toArray(new String[groups.size()]);
    }

    @Override
    @WebMethod
    public String getRelativePathAsString(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "uuid") String uuid) {
        WSRemotingSession rs = initSession(sessionId);
        DocumentModel doc = rs.getDocumentManager().getDocument(new IdRef(uuid));
        if (doc == null) {
            log.debug("Document not found for uuid=" + uuid);
            return "";
        } else {
            return doc.getPathAsString();
        }
    }

    private Map<String, Object> createDataMap(String[] propertiesArray) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < propertiesArray.length; i += 2) {
            String key = propertiesArray[i];
            String value = propertiesArray[i + 1];
            String[] path = key.split("\\.");

            createSubMaps(map, path, value, 0);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private void createSubMaps(Map<String, Object> map, String[] path, String value, int depth) {
        String key = path[depth];

        if (depth == path.length - 1) {
            map.put(key, value);
        } else {
            Map<String, Object> subMap = (Map<String, Object>) map.get(key);
            if (subMap == null) {
                subMap = new HashMap<String, Object>();
                map.put(path[depth], subMap);
            }
            createSubMaps(subMap, path, value, depth + 1);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @WebMethod
    public String uploadDocument(@WebParam(name = "sessionId") String sid,
            @WebParam(name = "parentUuid") String parentUUID, @WebParam(name = "type") String type,
            @WebParam(name = "properties") String[] properties) {
        // TODO Note: This method is intented to be a general method, but now it
        // can only be used by NuxeoCompanionForOffice
        // In the future, a new method (which will set the properties of a
        // document from a given map) will be probably
        // available in org.nuxeo.ecm.core.api.impl.DocumentHelper and then this
        // method will be made "general".

        WSRemotingSession rs = initSession(sid);
        String name = "file_" + System.currentTimeMillis();
        CoreSession documentManager = rs.getDocumentManager();
        DocumentRef parentRef = new IdRef(parentUUID);
        DocumentModel document = new DocumentModelImpl(documentManager.getDocument(parentRef).getPathAsString(), name,
                type);

        document = documentManager.createDocument(document);

        Map<String, Object> propertiesMap = createDataMap(properties);

        Map<String, Object> fileMap = (Map<String, Object>) propertiesMap.get("file");
        Map<String, Object> contentMap = (Map<String, Object>) fileMap.get("content");
        Map<String, Object> dublincoreMap = (Map<String, Object>) propertiesMap.get("dublincore");

        document.setProperty("dublincore", "description", dublincoreMap.get("description"));
        document.setProperty("dublincore", "title", dublincoreMap.get("title"));
        String filename = (String) fileMap.get("filename");
        final byte[] contentData = Base64.decodeBase64((String) contentMap.get("data"));
        // String contentType = (String) contentMap.get("mime-type") ;
        Blob blob = Blobs.createBlob(contentData);

        MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);

        String mimetype;
        try {
            mimetype = mimeService.getMimetypeFromFilenameAndBlobWithDefault(filename, blob, "");
        } catch (MimetypeDetectionException e) {
            log.error(String.format("error during mimetype detection for %s: %s", filename, e.getMessage()));
            mimetype = "";
        }

        String encoding = (String) contentMap.get("encoding");
        blob.setEncoding(encoding);
        blob.setMimeType(mimetype);
        document.setProperty("file", "content", blob);

        documentManager.saveDocument(document);
        documentManager.save();

        return "";
    }

}
