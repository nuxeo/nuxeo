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

import java.util.List;

import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.dws.DWSMetaData;
import org.nuxeo.wss.spi.dws.Site;

public interface WSSBackend {

    WSSListItem getItem(String location) throws WSSException;

    List<WSSListItem> listItems(String location) throws WSSException;

    List<WSSListItem> listFolderishItems(String location) throws WSSException;

    List<WSSListItem> listLeafItems(String location) throws WSSException;

    void begin() throws WSSException;

    void saveChanges() throws WSSException;

    void discardChanges() throws WSSException;

    WSSListItem moveItem(String oldLocation, String newLocation) throws WSSException;

    void removeItem(String location) throws WSSException;

    boolean exists(String location);

    WSSListItem createFolder(String location, String name) throws WSSException;

    WSSListItem createFileItem(String location, String name) throws WSSException;

    DWSMetaData getMetaData(String location, WSSRequest request) throws WSSException;

    Site getSite(String location) throws WSSException;
}
