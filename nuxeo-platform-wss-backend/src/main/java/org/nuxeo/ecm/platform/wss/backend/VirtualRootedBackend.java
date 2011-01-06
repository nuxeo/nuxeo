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
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.AbstractWSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dummy.DummyWSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

public class VirtualRootedBackend extends AbstractWSSBackend implements
        NuxeoWSSBackend {

    protected String root;
    protected NuxeoWSSBackend realBackend;
    protected WSSListItem rootItem;
    protected DummyWSSListItem nuxeoRoot;

    public VirtualRootedBackend(String root, NuxeoWSSBackend realBackend) {
        this.root = root;
        this.realBackend = realBackend;
        rootItem = new DummyWSSListItem("", "WSS Root", null);
        nuxeoRoot = new DummyWSSListItem(root, Framework.getProperty("org.nuxeo.ecm.instance.name","Nuxeo Server"), null);
        nuxeoRoot.markAsSite();
    }

    public void discardChanges() throws WSSException {
        realBackend.discardChanges();
    }

    protected String getRealBackEndPath(String location) {
        Path path = new Path(location);
        while (root.equals(path.segment(0))) { // strange !!!
            path = path.removeFirstSegments(1);
        }
        return path.toString();
    }

    public WSSListItem getItem(String location) throws WSSException {
        if ("".equals(location) || "/".equals(location)) {
            return rootItem;
        } else if (root.equals(location) || ("/" + root).equals(location) || ("/" + root + "/").equals(location)) {
            return nuxeoRoot;
        }
        return realBackend.getItem(getRealBackEndPath(location));
    }

    public List<WSSListItem> listItems(String location) throws WSSException {
        if ("".equals(location) || "/".equals(location)) {
            List<WSSListItem> children = new ArrayList<WSSListItem>();
            children.add(nuxeoRoot);
            return children;
        }
        return realBackend.listItems(getRealBackEndPath(location));
    }

    public WSSListItem moveItem(String oldLocation, String newLocation)
            throws WSSException {
        if ("".equals(oldLocation) || "/".equals(oldLocation)) {
            throw new WSSException("Root item can not be moved");
        } else if (root.equals(oldLocation) || ("/" + root).equals(oldLocation)) {
            throw new WSSException("Nuxeo root item can not be moved");
        }

        oldLocation = getRealBackEndPath(oldLocation);
        newLocation = getRealBackEndPath(newLocation);

        return realBackend.moveItem(oldLocation, newLocation);
    }

    public void removeItem(String location) throws WSSException {
        if ("".equals(location) || "/".equals(location)) {
            throw new WSSException("Root item can not be removed");
        } else if (root.equals(location) || ("/" + root).equals(location)) {
            throw new WSSException("Nuxeo root item can not be removed");
        }
        realBackend.removeItem(getRealBackEndPath(location));
    }

    @Override
    public boolean exists(String location) {
        if ("".equals(location) || "/".equals(location)) {
            return true;
        } else if (root.equals(location) || ("/" + root).equals(location)) {
            return true;
        }
        return realBackend.exists(getRealBackEndPath(location));
    }

    public WSSListItem createFolder(String parentPath, String name)
            throws WSSException {
        if ("".equals(parentPath) || "/".equals(parentPath)) {
            throw new WSSException("Root item can not be removed");
        } else if (root.equals(parentPath) || ("/" + root).equals(parentPath)) {
            throw new WSSException("Nuxeo root item can not be removed");
        }
        return realBackend.createFolder(getRealBackEndPath(parentPath), name);
    }

    public WSSListItem createFileItem(String parentPath, String name)
            throws WSSException {

        if ("".equals(parentPath) || "/".equals(parentPath)) {
            throw new WSSException("Root item can not be removed");
        } else if (root.equals(parentPath) || ("/" + root).equals(parentPath)) {
            throw new WSSException("Nuxeo root item can not be removed");
        }
        return realBackend.createFileItem(getRealBackEndPath(parentPath), name);
    }

    public void saveChanges(boolean release) throws WSSException {
        realBackend.saveChanges(release);
    }

    public void begin() throws WSSException {
        realBackend.begin();
    }

    public void saveChanges() throws WSSException {
        realBackend.saveChanges();
    }

    public void discardChanges(boolean release) throws WSSException {
        realBackend.discardChanges(release);
    }

    public DWSMetaData getMetaData(String location, WSSRequest request)
            throws WSSException {
        return realBackend.getMetaData(getRealBackEndPath(location), request);
    }

    public Site getSite(String location) throws WSSException {
        return realBackend.getSite(getRealBackEndPath(location));
    }

}
