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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

public class WSSVirtualBackendAdapter extends WSSBackendAdapter {

    @SuppressWarnings("unused")
    private String urlRoot;

    public WSSVirtualBackendAdapter(Backend backend, String virtualRoot) {
        super(backend, virtualRoot);
        this.urlRoot = virtualRoot + backend.getRootUrl();
    }

    @Override
    public boolean exists(String location) {
        return getBackend(location).exists(location);
    }

    @Override
    public WSSListItem getItem(String location) throws WSSException {
        return getBackend(location).getItem(location);
    }

    @Override
    public List<WSSListItem> listItems(String location) throws WSSException {
        return getBackend(location).listItems(location);
    }

    @Override
    public void begin() throws WSSException {
        // nothing
    }

    @Override
    public void saveChanges() throws WSSException {
        super.saveChanges();
    }

    @Override
    public WSSListItem moveItem(String location, String destination) throws WSSException {
        return getBackend(location).moveItem(location, destination);
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

    protected WSSBackend getBackend(String location){
        Backend backend = this.backend.getBackend(cleanLocation(location));
        if(backend == null){
            return new WSSFakeBackend();
        }
        return new WSSBackendAdapter(backend, virtualRoot);
    }
}
