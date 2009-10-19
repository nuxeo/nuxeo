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

import java.io.InputStream;
import java.util.Date;

import org.nuxeo.wss.WSSException;

public interface WSSListItem {

    public String getAuthor();

    public String getSubPath();

    public String getRelativeSubPath(String siteRootPath);

    public String getRelativeFilePath(String siteRootPath);

    public String getDescription();

    public void setDescription(String description);

    public InputStream getStream();

    public void setStream(InputStream stream, String fileName) throws WSSException;

    public String getName();

    public String getEtag();

    public String getType();

    public boolean isFolderish();

    public String getIcon();

    public void setIcon(String icon);


    public String getModifiedTS();


    public String getCreatedTS();


    public int getSize();

    public String getSizeAsString();

    public void checkOut(String userName) throws WSSException;

    public void uncheckOut(String userName) throws WSSException;

    public String getLastModificator();


    public String getCheckoutTS();

    public String getCheckoutExpiryTS();

    public String getCheckoutUser();

    public String getDisplayName();

    public boolean isSite();

    public Date getCreationDate();

    public Date getModificationDate();

    public boolean canCheckOut(String userName);

    public boolean canUnCheckOut(String userName);

    public boolean isCheckOut();

}