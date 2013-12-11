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

import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

public class WSSFakeBackend implements WSSBackend {

    @Override
    public WSSListItem getItem(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listItems(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listFolderishItems(String s) throws WSSException {
        return null;
    }

    @Override
    public List<WSSListItem> listLeafItems(String s) throws WSSException {
        return null;
    }

    @Override
    public void begin() throws WSSException {

    }

    @Override
    public void saveChanges() throws WSSException {

    }

    @Override
    public WSSListItem moveItem(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public void removeItem(String s) throws WSSException {

    }

    @Override
    public boolean exists(String s) {
        return false;
    }

    @Override
    public WSSListItem createFolder(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public WSSListItem createFileItem(String s, String s1) throws WSSException {
        return null;
    }

    @Override
    public DWSMetaData getMetaData(String s, WSSRequest wssRequest)
            throws WSSException {
        return null;
    }

    @Override
    public Site getSite(String s) throws WSSException {
        return null;
    }
}
