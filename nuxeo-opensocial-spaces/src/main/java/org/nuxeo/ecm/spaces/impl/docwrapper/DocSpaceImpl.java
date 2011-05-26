/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.impl.docwrapper;

import static org.nuxeo.ecm.spaces.api.Constants.UNIT_DOCUMENT_TYPE;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_POSITION_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_SCHEMA;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.server.layout.YUILayoutAdapter;
import org.nuxeo.opensocial.container.server.service.WebContentSaverService;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.runtime.api.Framework;

public class DocSpaceImpl implements Space {

    protected final DocumentModel doc;

    protected static final String SPACE_CATEGORY = "space:categoryId";

    private static final Log LOGGER = LogFactory.getLog(DocSpaceImpl.class);

    protected DocSpaceImpl(DocumentModel doc) {
        this.doc = doc;
    }

    public YUILayoutAdapter getLayout() throws ClientException {
        return doc.getAdapter(YUILayoutAdapter.class);
    }

    public String getCategory() throws ClientException {
        return (String) doc.getPropertyValue(SPACE_CATEGORY);
    }

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (ClientException e) {
            return "";
        }
    }

    public String getId() {
        return doc.getId();
    }

    public String getName() {
        return doc.getName();
    }

    public String getOwner() throws ClientException {
        return (String) doc.getPropertyValue("dc:creator");
    }

    public String getViewer() {
        return session().getPrincipal().getName();
    }

    public String getTitle() throws ClientException {
        return doc.getTitle();
    }

    private CoreSession session() {
        return doc.getCoreSession();
    }

    public Boolean hasPermission(String id, String permissionName)
            throws ClientException {
        DocumentModel document = session().getDocument(new IdRef(id));
        return session().hasPermission(document.getRef(), permissionName);
    }

    public List<String> getAvailableSecurityPermissions()
            throws ClientException {
        return session().getAvailableSecurityPermissions();
    }

    public boolean isReadOnly() throws ClientException {
        return session().hasPermission(doc.getRef(), SecurityConstants.WRITE);
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public Space copyFrom(Space space) throws ClientException {
        // setLayout(space.getLayout()); //TODO
        setDescription(space.getDescription());
        setTitle(space.getTitle());
        return this;
    }

    public void setDescription(String description) throws ClientException {
        doc.setPropertyValue("dc:description", description);
    }

    public void setTitle(String title) throws ClientException {
        doc.setPropertyValue("dc:title", title);
    }

    public void setCategory(String category) throws ClientException {
        doc.setPropertyValue(SPACE_CATEGORY, category);
    }

    public void save() throws ClientException {
        session().saveDocument(doc);
    }

    public void remove() throws ClientException {
        CoreSession session = doc.getCoreSession();
        session.removeDocument(doc.getRef());
    }

    public WebContentData createWebContent(WebContentData data)
            throws ClientException {
        WebContentSaverService service;
        try {
            service = Framework.getService(WebContentSaverService.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }
        IdRef idRef = new IdRef(data.getUnitId());
        // TODO test of the existence idRef should be done in the service !
        if (session().exists(idRef)) {
            DocumentModel unitDoc = session().getDocument(idRef);
            try {
                data = service.create(data, unitDoc.getId(), session());
            } catch (Exception e) {
                throw new ClientException("Unable to create web content", e);
            }
        } else {
            throw new ClientException("Unable to find unit for id "
                    + idRef.toString());
        }

        return data;
    }

    @SuppressWarnings("serial")
    public List<WebContentData> readWebContents() throws ClientException {
        Filter webContentFilter = new Filter() {
            public boolean accept(DocumentModel doc) {
                return doc.hasSchema(WEB_CONTENT_SCHEMA);
            }
        };

        Sorter webContentSorter = new Sorter() {
            public int compare(DocumentModel doc1, DocumentModel doc2) {
                Long pos1;
                Long pos2;
                try {
                    pos1 = (Long) doc1.getPropertyValue(WEB_CONTENT_POSITION_PROPERTY);
                    pos2 = (Long) doc2.getPropertyValue(WEB_CONTENT_POSITION_PROPERTY);
                    return pos1.compareTo(pos2);
                } catch (Exception e) {
                    LOGGER.error(e, e);
                    return 0;
                }
            }
        };

        WebContentSaverService service;
        try {
            service = Framework.getService(WebContentSaverService.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }

        List<WebContentData> webContentsList = new ArrayList<WebContentData>();

        for (DocumentModel unitDoc : session().getChildren(
                getDocument().getRef(), UNIT_DOCUMENT_TYPE)) {
            for (DocumentModel webContentDoc : session().getChildren(
                    unitDoc.getRef(), null, webContentFilter, webContentSorter)) {
                try {
                    webContentsList.add(service.read(webContentDoc, session()));
                } catch (Exception e) {
                    throw new ClientException("Unable to get all web contents",
                            e);
                }
            }
        }
        return webContentsList;
    }

    public WebContentData updateWebContent(WebContentData data)
            throws ClientException {
        WebContentSaverService service = null;

        try {
            service = Framework.getService(WebContentSaverService.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }

        try {
            data = service.update(data, session());
        } catch (Exception e) {
            throw new ClientException("Unable to update web content", e);
        }

        return data;
    }

    public void deleteWebContent(WebContentData data) throws ClientException {
        WebContentSaverService service;
        try {
            service = Framework.getService(WebContentSaverService.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }
        try {
            service.delete(data, session());
        } catch (Exception e) {
            throw new ClientException("Unable to delete web content", e);
        }
    }

    public void initLayout(YUILayout layout) throws ClientException {
        getLayout().initLayout(layout);
    }

    public void moveWebContent(WebContentData data, String dstUnitName)
            throws ClientException {
        DocumentModel doc = getDocForData(data);
        session().move(doc.getRef(), new IdRef(dstUnitName), null);
    }

    public DocumentModel getDocForData(WebContentData data)
            throws ClientException {
        IdRef idRef = new IdRef(data.getId());
        if (session().exists(idRef)) {
            return session().getDocument(idRef);
        } else {
            throw new ClientException("Enable to find data for id "
                    + idRef.toString());
        }
    }

    public WebContentData getWebContent(String webContentId)
            throws ClientException {
        WebContentSaverService service;
        try {
            service = Framework.getService(WebContentSaverService.class);
        } catch (Exception e) {
            throw new ClientException("Unable to get Space Manager", e);
        }
        DocumentModel webContentDoc = session().getDocument(
                new IdRef(webContentId));
        try {
            return service.read(webContentDoc, session());
        } catch (Exception e) {
            throw new ClientException("Unable to retrieve web content", e);
        }
    }

    public Calendar getPublicationDate() throws ClientException {
        return (Calendar) doc.getPropertyValue("dc:valid");
    }

    public void setPublicationDate(Calendar datePublication)
            throws ClientException {
        doc.setPropertyValue("dc:valid", datePublication);
    }

    // TODO ******************************************************************

    public Map<String, Map<String, Boolean>> getPermissions()
    throws ClientException {
        return getPermissions((List<WebContentData>) null);
    }

    public Map<String, Map<String, Boolean>> getPermissions(List<WebContentData> list)
            throws ClientException {
        Map<String, Map<String, Boolean>> rights = new HashMap<String, Map<String, Boolean>>();

        // Look permissions for current space
        Map<String, Boolean> spaceRight = getPermissions(doc.getId());
        rights.put(doc.getId(), spaceRight);

        // TODO STUB Get every gadgets in the space and assign the space's
        // permission
        // value
        if (list == null) {
            list = readWebContents();
        }
        for (WebContentData data : list) {
            rights.put(data.getId(), spaceRight);
        }

        return rights;
    }

    public Map<String, Boolean> getPermissions(String id)
            throws ClientException {
        // TODO Stub for everything we need we assign the space's permission
        id = doc.getId();

        Map<String, Boolean> perm = new HashMap<String, Boolean>();

        if (hasPermission(id, SecurityConstants.EVERYTHING)) {
            perm.put(PermissionsConstants.EVERYTHING, Boolean.TRUE);
        } else {
            perm.put(PermissionsConstants.READ, Boolean.TRUE);
        }

        return perm;
    }

}
