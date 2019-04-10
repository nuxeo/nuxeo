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

package org.nuxeo.wss.spi.dummy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.spi.AbstractWSSListItem;
import org.nuxeo.wss.spi.WSSListItem;

public class DummyWSSListItem extends AbstractWSSListItem implements WSSListItem {

    private static final Log log = LogFactory.getLog(DummyWSSListItem.class);

    protected String subPath;

    protected String description;

    protected InputStream stream;

    protected String name;

    protected String type;

    protected int size;

    protected String binaryResourcePath;

    protected byte[] byteArray;

    protected String uuid = null;

    protected Calendar creationDate;

    protected Calendar modificationDate;

    protected Calendar checkoutDate;

    protected String checkoutUser;

    public DummyWSSListItem(String name, String description, InputStream is) {
        this(name, description, null, is);
    }

    public DummyWSSListItem(String name, String description, String basePath, InputStream is) {
        this.name = name;
        this.description = description;
        creationDate = Calendar.getInstance();
        if (basePath != null) {
            if (basePath.endsWith("/")) {
                this.subPath = basePath + name;
            } else {
                this.subPath = basePath + "/" + name;
            }
        } else {
            this.subPath = name;
        }
        try {
            setStream(is, null);
        } catch (WSSException e) {
            log.error("Error processing stream", e);
        }
    }

    public String getSubPath() {
        return subPath;
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        update();
    }

    public InputStream getStream() {
        if (stream == null && byteArray != null) {
            return new ByteArrayInputStream(byteArray);
        }
        if (stream == null && binaryResourcePath != null) {
            return DummyMemoryTree.class.getClassLoader().getResourceAsStream(binaryResourcePath);
        }
        return stream;
    }

    public void setStream(InputStream stream, String fileName) throws WSSException {
        if (stream == null) {
            type = "folder";
        } else {
            type = "file";
            try {
                copyStreamToBA(stream);
            } catch (IOException e) {
                throw new WSSException("Error while transfering stream", e);
            }
        }
        update();
    }

    protected void update() {
        modificationDate = Calendar.getInstance();
    }

    protected void copyStreamToBA(InputStream is) throws IOException {
        byte[] tmp_buffer = new byte[512 * 1024];
        byte[] read_buffer = new byte[3];
        int i = 0;
        int idx = 0;
        while ((i = is.read(read_buffer)) > 0) {
            System.arraycopy(read_buffer, 0, tmp_buffer, idx, i);
            idx = idx + i;
        }

        byteArray = new byte[idx];
        System.arraycopy(tmp_buffer, 0, byteArray, 0, idx);
        size = idx;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEtag() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    public String getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreationDate() {
        return creationDate.getTime();
    }

    public Date getModificationDate() {
        if (modificationDate == null) {
            return getCreationDate();
        }
        return modificationDate.getTime();
    }

    public void setBinaryResourcePath(String binaryResourcePath) {
        this.binaryResourcePath = binaryResourcePath;
        this.type = "file";
    }

    public String getLastModificator() {
        return "someone";
    }

    public void checkOut(String userName) throws WSSException {
        if (checkoutUser == null || checkoutUser.equals(userName)) {
            checkoutUser = userName;
        } else {
            throw new WSSException("Document is already checkedout");
        }
    }

    public void uncheckOut(String userName) throws WSSException {
        if (checkoutUser == null || checkoutUser.equals(userName)) {
            checkoutUser = null;
        } else {
            throw new WSSException("Document is checkedout by another user");
        }
    }

    @Override
    protected Date getCheckoutDate() {
        if (checkoutDate == null) {
            checkoutDate = Calendar.getInstance();
            ;
        }
        return checkoutDate.getTime();
    }

    @Override
    protected Date getCheckoutExpiryDate() {
        Calendar date = Calendar.getInstance();
        date.setTime(getCheckoutDate());
        date.add(Calendar.MINUTE, 10);
        return date.getTime();
    }

    public String getCheckoutUser() {
        return checkoutUser;
    }

    public void markAsSite() {
        isSiteItem = true;
    }

    public String getAuthor() {
        return "me";
    }

}
