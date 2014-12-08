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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dummy.DummyWSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

public class WSSRootBackendAdapter extends WSSBackendAdapter {

    private static final Log log = LogFactory.getLog(WSSRootBackendAdapter.class);

    public WSSRootBackendAdapter(Backend backend, String virtualRoot) {
        super(backend, virtualRoot);
        this.urlRoot = virtualRoot + backend.getRootUrl();
    }

    @Override
    public boolean exists(String location) {
        return getBackend(location).exists(location);
    }

    @Override
    public WSSListItem getItem(String location) throws WSSException {
        if ("".equals(location) || "/".equals(location)) {
            return new DummyWSSListItem("", "WSS Root", null);
        }
        return getBackend(location).getItem(location);
    }

    @Override
    public List<WSSListItem> listItems(String location) throws WSSException {
        WSSBackend backend = getBackend(location);
        return backend.listItems(location);
    }

    @Override
    public void begin() throws WSSException {
        // backend.begin();
    }

    @Override
    public void saveChanges() throws WSSException {
        super.saveChanges();
    }

    @Override
    public WSSListItem moveItem(String location, String destination) throws WSSException {
        WSSBackend sourceBackend = getBackend(location);
        DocumentModel source = null;
        if (sourceBackend instanceof WSSBackendAdapter) {
            source = ((WSSBackendAdapter) sourceBackend).getDocument(location);
        }
        if (source == null) {
            throw new WSSException("Can't move document. Source did not found.");
        }
        WSSBackend destinationBackend = getBackend(destination);
        if (destinationBackend instanceof WSSBackendAdapter) {
            return ((WSSBackendAdapter) destinationBackend).moveItem(source, destination);
        } else {
            return sourceBackend.moveItem(location, destination);
        }
    }

    @Override
    public void removeItem(String location) throws WSSException {
        getBackend(location).removeItem(location);
    }

    @Override
    public WSSListItem createFolder(String parentPath, String name) throws WSSException {
        return getBackend(parentPath).createFolder(parentPath, name);
    }

    @Override
    public WSSListItem createFileItem(String parentPath, String name) throws WSSException {
        return getBackend(parentPath).createFileItem(parentPath, name);
    }

    @Override
    public DWSMetaData getMetaData(String location, WSSRequest wssRequest) throws WSSException {
        return getBackend(location).getMetaData(location, wssRequest);
    }

    @Override
    public Site getSite(String location) throws WSSException {
        return getBackend(location).getSite(location);
    }

    protected WSSBackend getBackend(String location) {
        try {
            Set<String> names = new HashSet<String>(this.backend.getVirtualFolderNames());
            Path locationPath = new Path(location);
            String[] segments = locationPath.segments();
            int removeSegments = 0;
            for (String segment : segments) {
                if (names.contains(segment)) {
                    break;
                } else {
                    removeSegments++;
                }
            }
            Path localVirtualRootPath = locationPath.removeLastSegments(locationPath.segmentCount() - removeSegments);
            virtualRoot = cleanPath(localVirtualRootPath.toString());
        } catch (ClientException e) {
            log.warn("Error during resolve virtual root");
        }

        location = cleanLocation(location);
        Backend backend = this.backend.getBackend(location);
        if (backend == null) {
            return new WSSFakeBackend();
        }
        if (backend.isVirtual()) {
            return new WSSVirtualBackendAdapter(backend, virtualRoot);
        }
        return new WSSBackendAdapter(backend, virtualRoot);
    }

}
