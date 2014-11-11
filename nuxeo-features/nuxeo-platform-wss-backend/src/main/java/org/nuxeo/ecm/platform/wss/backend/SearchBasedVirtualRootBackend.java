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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.wss.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.wss.service.WSSPlugableBackendManager;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.DWSMetaDataImpl;
import org.nuxeo.wss.spi.dws.Site;

public class SearchBasedVirtualRootBackend extends AbstractNuxeoCoreBackend implements
        WSSBackend {

    private static final Log log = LogFactory.getLog(SearchBasedVirtualRootBackend.class);

    protected Map<String, String> name2path = null;
    protected Map<String, SimpleNuxeoBackend> name2backend = new HashMap<String, SimpleNuxeoBackend>();

    protected String urlRoot;

    protected String query = "select * from Workspace where ecm:mixinType != 'HiddenInNavigation' AND  ecm:currentLifeCycleState != 'deleted' AND ecm:isProxy = 0 order by ecm:path";

    public SearchBasedVirtualRootBackend(String urlRoot, String query) {
        this.urlRoot = urlRoot;
        if (query != null) {
            this.query = query;
        }
    }

    protected SimpleNuxeoBackend getBackend(String name) throws WSSException {
        try {
            SimpleNuxeoBackend backend = name2backend.get(name);
            if (backend == null) {
                String path = getName2path().get(name);
                if (path == null) {
                    throw new WSSException("unable to resolve path");
                }
                backend = new SimpleNuxeoBackend(path, urlRoot, getCoreSession());
                name2backend.put(name, backend);
            }
            return backend;
        }
        catch (Exception e) {
            throw new WSSException("Unable to get backend", e);
        }
    }

    protected WSSListItem createNode(String parentPath, String name, boolean folderish) throws WSSException {

        WSSListItem item;
        Path containerPath = new Path(parentPath);
        if (containerPath.segmentCount() == 0) {
            throw new WSSException("Can not create item at root");
        }

        String base = containerPath.segment(0);

        if (folderish) {
            item = getBackend(base).createFolder(containerPath.removeFirstSegments(1).toString(), name);
        } else {
            item = getBackend(base).createFileItem(containerPath.removeFirstSegments(1).toString(), name);
        }

        ((NuxeoListItem) item).setVirtualRootNodeName(base);

        return item;
    }


    public WSSListItem createFileItem(String location, String name)
            throws WSSException {
        return createNode(location, name, false);
    }

    public WSSListItem createFolder(String location, String name)
            throws WSSException {
        return createNode(location, name, true);
    }

    public WSSListItem getItem(String location) throws WSSException {
        Path path = new Path(location);
        String base = path.segment(0);
        if (path.segmentCount() == 1) {
            try {
                String corePath = getName2path().get(base);
                DocumentModel doc = getCoreSession().getDocument(new PathRef(corePath));
                NuxeoListItem item =  WSSPlugableBackendManager.instance().createItem(doc, new Path(corePath).removeLastSegments(1).toString(), urlRoot);
                item.setVirtualName(base);
                return item;
            } catch (Exception e) {
                throw new WSSException("unable to resolve path", e);
            }
        } else {
            WSSListItem item = getBackend(base).getItem(path.removeFirstSegments(1).toString());
            ((NuxeoListItem) item).setVirtualRootNodeName(base);
            return item;
        }
    }

    public List<WSSListItem> listItems(String location) throws WSSException {
        Path path = new Path(location);
        String base = path.segment(0);

        if ("/".equals(base) || base == null) {
            List<WSSListItem> items = new ArrayList<WSSListItem>();
            try {
                List<String> names = new ArrayList<String>();
                names.addAll(getName2path().keySet());
                Collections.sort(names);
                for (String name : names) {
                    // XXX !!!
                    String corePath = getName2path().get(name);
                    DocumentModel doc = getCoreSession().getDocument(new PathRef(corePath));
                    NuxeoListItem item = WSSPlugableBackendManager.instance().createItem(doc, new Path(corePath).removeLastSegments(1).toString(), urlRoot);
                    item.setVirtualName(name);
                    items.add(item);
                }
                return items;
            } catch (Exception e) {
                throw new WSSException("error in listItems", e);
            }
        } else {
            List<WSSListItem> items = getBackend(base).listItems(path.removeFirstSegments(1).toString());
            for (WSSListItem item : items) {
                ((NuxeoListItem) item).setVirtualRootNodeName(base);
            }
            return items;
        }
    }

    public WSSListItem moveItem(String oldLocation, String newLocation) throws WSSException {
        Path path = new Path(oldLocation);
        String base = path.segment(0);
        if (path.segmentCount() == 1) {
            throw new WSSException("can not move this item");
        } else {
            String baseDest = new Path(newLocation).segment(0);
            if (base.equals(baseDest)) {
                // move within the same workspace
                WSSListItem item = getBackend(base).moveItem(path.removeFirstSegments(1).toString(), new Path(newLocation).removeFirstSegments(1).toString());
                ((NuxeoListItem) item).setVirtualRootNodeName(base);
                return item;
            } else {
                // move from one workspace to an other
                NuxeoListItem sourceItem = (NuxeoListItem) getBackend(base).getItem(path.removeFirstSegments(1).toString());
                DocumentModel sourceDocument = sourceItem.getDoc();
                WSSListItem item = getBackend(baseDest).moveDocument(sourceDocument, new Path(newLocation).removeFirstSegments(1).toString());
                ((NuxeoListItem) item).setVirtualRootNodeName(baseDest);
                return item;
            }
        }
    }

    public void removeItem(String location) throws WSSException {
        Path path = new Path(location);
        String base = path.segment(0);
        if (path.segmentCount() == 1) {
            throw new WSSException("can not move this item");
        } else {
            getBackend(base).removeItem(path.removeFirstSegments(1).toString());
        }
    }

    private boolean isHead(String path, List<String> paths, int idx) {
        int level = new Path(path).segmentCount();

        for (int i = idx; i >= 0; i--) {
            String other = paths.get(i);
            if (path.contains(other)) {
                if (new Path(other).segmentCount() == level - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<String, String> getName2path() throws ClientException, Exception {
        if (name2path == null) {
            name2path = new HashMap<String, String>();
            DocumentModelList docs = getCoreSession().query(query);
            List<String> paths = new ArrayList<String>();
            for (DocumentModel doc : docs) {
                paths.add(doc.getPathAsString());
            }

            List<String> heads = new ArrayList<String>();
            for (int idx = 0; idx < paths.size(); idx++) {
                String path = paths.get(idx);
                if (isHead(path, paths, idx)) {
                    heads.add(path);
                }
            }

            for (String head : heads) {
                String headName = new Path(head).lastSegment();
                String name = headName;
                int idx = 1;
                while (name2path.containsKey(name)) {
                    name = headName + "-" + idx;
                    idx = idx + 1;
                }
                name2path.put(name, head);
            }
        }
        return name2path;
    }

    protected Map<String, SimpleNuxeoBackend> getName2backend() {
        return name2backend;
    }

    public DWSMetaData getMetaData(String url, WSSRequest request)
            throws WSSException {
        Path path = new Path(url);
        String base = path.segment(0);
        if (path.segmentCount() == 1) {
            throw new WSSException("unable to resolve path");
        } else {

            DWSMetaDataImpl metadata = (DWSMetaDataImpl) getBackend(base).getMetaData(path.removeFirstSegments(1).toString(), request);
            List<WSSListItem> docs = metadata.getDocuments();
            for (WSSListItem item : docs) {
                ((NuxeoListItem) item).setVirtualRootNodeName(base);
            }

            WSSListItem siteItem = metadata.getSite().getItem();
            ((NuxeoListItem) siteItem).setVirtualRootNodeName(base);

            return metadata;
        }
    }

    public Site getSite(String location) throws WSSException {
        Path path = new Path(location);
        String base = path.segment(0);
        if (path.segmentCount() == 1) {
            throw new WSSException("unable to resolve path");
        } else {
            return getBackend(base).getSite(path.removeFirstSegments(1).toString());
        }
    }

}
