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
 *     Gagnavarslan ehf
 */
package org.nuxeo.wss.impl;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.common.utils.Path;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.spi.AbstractWSSListItem;
import org.nuxeo.wss.spi.WSSListItem;

public class VirtualListItem extends AbstractWSSListItem implements WSSListItem {

    protected String name;

    protected String corePathPrefix;

    protected String urlRoot;

    public VirtualListItem(String name, String corePathPrefix, String urlRoot) {
        this.name = name;
        this.corePathPrefix = corePathPrefix;
        this.urlRoot = urlRoot;
    }

    @Override
    protected Date getCheckoutDate() {
        return Calendar.getInstance().getTime();
    }

    @Override
    protected Date getCheckoutExpiryDate() {
        Date to = getCheckoutDate();
        if (to != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(to);
            cal.add(Calendar.MINUTE, 20);
            return cal.getTime();
        }
        return Calendar.getInstance().getTime();
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getSubPath() {
        Path completePath = new Path(urlRoot);
        completePath = completePath.append(name);
        String path = completePath.toString();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public void setDescription(String s) {
        // nothing
    }

    @Override
    public InputStream getStream() {
        return null;
    }

    @Override
    public void setStream(InputStream inputStream, String s)
            throws WSSException {
        // nothing
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEtag() {
        return name;
    }

    @Override
    public String getType() {
        return "folder";
    }

    @Override
    public int getSize() {
        return 10;
    }

    @Override
    public void checkOut(String s) throws WSSException {
        // nothing
    }

    @Override
    public void uncheckOut(String s) throws WSSException {
        // nothing
    }

    @Override
    public String getLastModificator() {
        return null;
    }

    @Override
    public String getCheckoutUser() {
        return "";
    }

    @Override
    public Date getCreationDate() {
        return Calendar.getInstance().getTime();
    }

    @Override
    public Date getModificationDate() {
        return Calendar.getInstance().getTime();
    }
}
