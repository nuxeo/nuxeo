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

package org.nuxeo.wss.spi;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.wss.WSSException;

public abstract class AbstractWSSBackend implements WSSBackend {

    @Override
    public List<WSSListItem> listFolderishItems(String location) throws WSSException {

        List<WSSListItem> folderish = new ArrayList<WSSListItem>();
        List<WSSListItem> all = listItems(location);
        for (WSSListItem item : all) {
            if (item.isFolderish()) {
                folderish.add(item);
            }
        }
        return folderish;
    }

    @Override
    public List<WSSListItem> listLeafItems(String location) throws WSSException {
        List<WSSListItem> leafs = new ArrayList<WSSListItem>();
        List<WSSListItem> all = listItems(location);
        for (WSSListItem item : all) {
            if (!item.isFolderish()) {
                leafs.add(item);
            }
        }
        return leafs;
    }

    @Override
    public boolean exists(String location) {
        try {
            WSSListItem item = getItem(location);
            if (item == null) {
                return false;
            } else {
                return true;
            }
        } catch (WSSException e) {
            return false;
        }
    }

}
