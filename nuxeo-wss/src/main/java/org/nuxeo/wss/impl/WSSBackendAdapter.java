/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 */
package org.nuxeo.wss.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.VirtualBackend;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSFilter;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.AbstractWSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.DWSMetaDataImpl;
import org.nuxeo.wss.spi.dws.Site;
import org.nuxeo.wss.spi.dws.SiteImpl;
import org.nuxeo.wss.spi.dws.User;
import org.nuxeo.wss.spi.dws.UserImpl;

public class WSSBackendAdapter extends AbstractWSSBackend {

    protected String corePathPrefix;

    protected String urlRoot;

    protected String virtualRoot;

    protected Backend backend;

    public WSSBackendAdapter(Backend backend, String virtualRoot) {
        this.backend = backend;
        this.corePathPrefix = backend.getRootPath();
        this.urlRoot = virtualRoot + "/" + backend.getRootUrl();
        this.virtualRoot = virtualRoot;
    }

    @Override
    public boolean exists(String location) {
        return backend.exists(cleanLocation(location));
    }

    @Override
    public WSSListItem getItem(String location) throws WSSException {
        location = cleanLocation(location);
        try {
            DocumentModel doc = backend.resolveLocation(location);
            if (doc != null) {
                return createItem(doc);
            } else {
                throw new WSSException("Unable to find item " + location);
            }
        } catch (ClientException e) {
            throw new WSSException("Error while getting item", e);
        }
    }

    @Override
    public List<WSSListItem> listItems(String location) throws WSSException {
        location = cleanLocation(location);
        List<WSSListItem> result = new ArrayList<WSSListItem>();
        try {
            if (backend.isVirtual()) {
                List<String> backendNames = ((VirtualBackend) backend).getOrderedBackendNames();
                for (String name : backendNames) {
                    result.add(createItem(name));
                }
            } else {
                DocumentModel parent = backend.resolveLocation(location);
                if (parent == null) {
                    throw new WSSException("Parent document with location " + location + " not found");
                }
                List<DocumentModel> children = backend.getChildren(parent.getRef());
                for (DocumentModel model : children) {
                    NuxeoListItem item = createItem(model);
                    result.add(item);
                }
            }
        } catch (ClientException e) {
            throw new WSSException("Error while getting children for " + location, e);
        }
        return result;
    }

    @Override
    public void begin() throws WSSException {

    }

    @Override
    public void saveChanges() throws WSSException {
        try {
            backend.saveChanges();
        } catch (ClientException e) {
            throw new WSSException("Error during save changes", e);
        }
    }

    @Override
    public WSSListItem moveItem(String location, String destination) throws WSSException {
        location = cleanLocation(location);
        destination = cleanLocation(destination);
        try {
            DocumentModel model = backend.resolveLocation(location);
            if (model == null) {
                throw new WSSException("Can't move document. Source did not found.");
            }
            Path destinationPath = backend.parseLocation(destination);
            DocumentModel doc = backend.moveItem(model, new PathRef(destinationPath.removeLastSegments(1).toString()),
                    destinationPath.lastSegment());
            return createItem(doc);
        } catch (ClientException e) {
            throw new WSSException("Error during move document", e);
        }
    }

    public WSSListItem moveItem(DocumentModel model, String destination) throws WSSException {
        destination = cleanLocation(destination);
        try {
            if (model == null) {
                throw new WSSException("Can't move document. Source did not found.");
            }
            Path destinationPath = backend.parseLocation(destination);
            DocumentModel doc = backend.moveItem(model, new PathRef(destinationPath.removeLastSegments(1).toString()),
                    destinationPath.lastSegment());
            return createItem(doc);
        } catch (ClientException e) {
            throw new WSSException("Error during move document", e);
        }
    }

    public DocumentModel getDocument(String location) throws WSSException {
        location = cleanLocation(location);
        try {
            return backend.resolveLocation(location);
        } catch (ClientException e) {
            throw new WSSException("Error during get document " + location);
        }
    }

    @Override
    public void removeItem(String location) throws WSSException {
        location = cleanLocation(location);
        try {
            backend.removeItem(location);
        } catch (ClientException e) {
            throw new WSSException("Error while deleting doc. Location:" + location, e);
        }
    }

    @Override
    public WSSListItem createFolder(String parentPath, String name) throws WSSException {
        parentPath = cleanLocation(parentPath);
        try {
            DocumentModel model = backend.createFolder(parentPath, name);
            return createItem(model);
        } catch (ClientException e) {
            throw new WSSException("Error child creating new folder", e);
        }
    }

    @Override
    public WSSListItem createFileItem(String parentPath, String name) throws WSSException {
        parentPath = cleanLocation(parentPath);
        try {
            DocumentModel model = backend.createFile(parentPath, name);
            return createItem(model);
        } catch (ClientException e) {
            throw new WSSException("Error child creating new file", e);
        }
    }

    @Override
    public DWSMetaData getMetaData(String location, WSSRequest wssRequest) throws WSSException {
        location = cleanLocation(location);
        try {
            DWSMetaDataImpl metadata = new DWSMetaDataImpl();

            String parentPath = new Path(location).removeLastSegments(1).toString();
            List<WSSListItem> documents = listItems(parentPath);

            metadata.setDocuments(documents);

            metadata.setSite(getSite(location));
            String cUserName = backend.getSession().getPrincipal().getName();

            List<String> userNames = new ArrayList<String>();
            for (WSSListItem item : documents) {
                if (item.getAuthor() != null && !userNames.contains(item.getAuthor())) {
                    userNames.add(item.getAuthor());
                }
                String[] contributors = (String[]) (((NuxeoListItem) item).getDoc().getPropertyValue("dc:contributors"));
                if (contributors != null) {
                    for (String contributor : contributors) {
                        if (!userNames.contains(contributor)) {
                            userNames.add(contributor);
                        }
                    }
                }
            }

            User currentUser = getUserFromLogin(cUserName, 1);
            metadata.setCurrentUser(currentUser);

            // manage users
            List<User> users = new ArrayList<User>();
            users.add(currentUser);
            int i = 2;
            for (String name : userNames) {
                if (!name.equals(cUserName)) {
                    users.add(getUserFromLogin(name, i));
                    i++;
                }
            }
            metadata.setUsers(users);

            return metadata;

        } catch (ClientException e) {
            throw new WSSException("Error in getMetadata", e);
        }
    }

    @Override
    public Site getSite(String location) throws WSSException {
        String parentPath = new Path(location).removeLastSegments(1).toString();
        NuxeoListItem parent = (NuxeoListItem) getItem(parentPath);
        String siteName = parent.getDisplayName();
        SiteImpl site = new SiteImpl(siteName);
        String nxUrl = urlRoot + "/nxpath/default" + parent.getDoc().getPathAsString() + "@view_documents";
        try {
            site.setAccessUrl("?" + WSSFilter.FILTER_FORWARD_PARAM + "=" + URLEncoder.encode(nxUrl, "UTF-8"));
            // site.setAccessUrl(URLEncoder.encode(nxUrl, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new WSSException("Error encoding url", e);
        }
        // site.setAccessUrl(parent.getRelativeFilePath(""));
        // site.setAccessUrl();
        site.setUserManagementUrl("");
        site.setListUUID(parent.getEtag());
        site.setItem(parent);
        return site;
    }

    private NuxeoListItem createItem(DocumentModel model) {
        return new NuxeoListItem(model, corePathPrefix, urlRoot);
    }

    private VirtualListItem createItem(String name) {
        return new VirtualListItem(name, corePathPrefix, urlRoot);
    }

    protected User getUserFromLogin(String userLogin, int idx) {

        User user = null;
        UserManager um = Framework.getService(UserManager.class);
        NuxeoPrincipal principal = um.getPrincipal(userLogin);
        if (principal != null) {
            String email = (String) principal.getModel().getProperty(um.getUserSchemaName(), um.getUserEmailField());
            String fullname = principal.getFirstName() + " " + principal.getLastName();
            if (fullname.equals(" ")) {
                fullname = userLogin;
            }
            user = new UserImpl("" + idx, userLogin, fullname, email);
        } else {
            user = new UserImpl("" + idx, userLogin, userLogin, "");
        }
        return user;
    }

    protected String cleanLocation(String location) {
        location = cleanPath(location);
        if (location.startsWith(virtualRoot)) {
            location = location.substring(virtualRoot.length());
        }
        return location;
    }

    protected String cleanPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
