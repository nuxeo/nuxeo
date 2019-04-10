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

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.spi.AbstractWSSListItem;
import org.nuxeo.wss.spi.WSSListItem;

public class NuxeoListItem extends AbstractWSSListItem implements WSSListItem {

    private static final Log log = LogFactory.getLog(NuxeoListItem.class);

    protected DocumentModel doc;
    protected String corePathPrefix;
    protected String urlRoot;
    protected String virtualName = null;
    protected String virtualRootNodeName = null;

    protected CoreSession getSession() {
        return CoreInstance.getInstance().getSession(doc.getSessionId());
    }

    public NuxeoListItem(DocumentModel doc, String corePathPrefix, String urlRoot) {
        this.doc = doc;
        this.corePathPrefix = corePathPrefix;
        this.urlRoot = urlRoot;
    }

    @Override
    protected Date getCheckoutDate() {
        String existingLock = null;
        try {
            existingLock = getSession().getLock(doc.getRef());
        } catch (ClientException e) {
            log.error("Unable to get lock", e);
        }
        if (existingLock != null) {
            String[] info = existingLock.split(":");
            if (info.length == 2) {
                String dateStr = info[1];
                try {
                    return DateFormat.getDateInstance(DateFormat.MEDIUM).parse(
                            dateStr);
                } catch (ParseException e) {
                    log.error("Unable to parse date", e);
                }
            }
        }
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

    public Date getCreationDate() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:created");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get creation date", e);
        }
        return Calendar.getInstance().getTime();
    }

    public Date getModificationDate() {
        try {
            Calendar modified = (Calendar) doc.getPropertyValue("dc:modified");
            if (modified != null) {
                return modified.getTime();
            }
        } catch (ClientException e) {
            log.error("Unable to get modification date", e);
        }
        return Calendar.getInstance().getTime();
    }

    public void checkOut(String userName) throws WSSException {
        try {
            String lock = getSession().getLock(doc.getRef());
            if (lock == null) {
                String lockDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
                String lockToken = userName + ":" + lockDate;
                getSession().setLock(doc.getRef(), lockToken);
            } else {
                if (!userName.equals(getCheckoutUser())) {
                    throw new WSSException("Document is already locked by another user");
                }
            }
        } catch (ClientException e) {
            throw new WSSException("Error while locking", e);
        }
    }

    public String getCheckoutUser() {

        String existingLock = null;
        try {
            existingLock = getSession().getLock(doc.getRef());
        } catch (ClientException e) {
            log.error("Unable to lock", e);
        }
        if (existingLock != null) {
            String[] info = existingLock.split(":");
            return info[0];
        }
        return null;
    }

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (Exception e) {
            log.error("Unable to get description", e);
            return "";
        }
    }

    public String getEtag() {
        return doc.getId();
    }

    public String getLastModificator() {
        return null;
    }

    public String getName() {
        if (virtualName != null) {
            return virtualName;
        }
        return doc.getName();
    }

    public int getSize() {
        int size = 0;
        if (!doc.isFolder()) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                try {
                    Blob blob = bh.getBlob();
                    if (blob != null) {
                        size = (int) blob.getLength();
                    }
                } catch (ClientException e) {
                    log.error("Unable to get blob Size", e);
                }
            }
        }
        return size;
    }

    public InputStream getStream() {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                return bh.getBlob().getStream();
            } catch (Exception e) {
                log.error("Unable to get Stream", e);
            }
        }
        return null;
    }

    public String getSubPath() {

        String path = doc.getPathAsString();
        if (corePathPrefix != null) {
            path = path.replace(corePathPrefix, "");
        }
        if (virtualName != null) {
            Path vPath = new Path(path);
            vPath = vPath.removeFirstSegments(1);
            path = new Path(virtualName).append(vPath).toString();
        } else if (virtualRootNodeName != null) {
            path = new Path(virtualRootNodeName).append(path).toString();
        }
        Path completePath = new Path(urlRoot);
        completePath = completePath.append(path);
        path = completePath.toString();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    @Override
    public String getRelativeFilePath(String siteRootPath) {
        String path = getRelativeSubPath(siteRootPath);
        if (!doc.isFolder()) {
            String filename = getFileName();
            if (filename != null) {
                // XXX : check for duplicated names
                path = new Path(path).removeLastSegments(1).append(filename).toString();
            }
        }
        return path;
    }

    protected String getFileName() {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    return blob.getFilename();
                }
            } catch (ClientException e) {
                log.error("Unable to get filename", e);
            }
        }
        return null;
    }

    public String getType() {
        if (doc.isFolder()) {
            return "folder";
        }
        return "file";
    }

    public void setDescription(String description) {
        try {
            doc.setPropertyValue("dc:description", description);
        } catch (Exception e) {
            log.error("Error while setting description", e);
        }
    }

    public void setStream(InputStream is, String fileName) throws WSSException {
        if (doc.hasSchema("file")) {
            //Blob blob = new InputStreamBlob(is);
            Blob blob = StreamingBlob.createFromStream(is);
            if (fileName != null) {
                blob.setFilename(fileName);
            }
            try {
                Blob oldBlob = (Blob) doc.getProperty("file", "content");
                if (oldBlob == null) {
                    // force to recompute icon
                    if (doc.hasSchema("common")) {
                        doc.setProperty("common", "icon", null);
                    }
                }
                doc.setProperty("file", "content", blob);
                doc.setProperty("file", "filename", fileName);
                doc = getSession().saveDocument(doc);
            } catch (ClientException e) {
                log.error("Error while setting stream", e);
            }
        } else {
            // XXX needs writable BlobHolder
            log.error("Update of type " + doc.getType() + " is not supported for now");
        }
    }

    public void uncheckOut(String userName) throws WSSException {
        try {
            String lock = getSession().getLock(doc.getRef());
            if (lock != null) {
                if (userName.equals(getCheckoutUser())) {
                    getSession().unlock(doc.getRef());
                } else {
                    throw new WSSException("Document is locked by another user");
                }
            }
        } catch (ClientException e) {
            throw new WSSException("Error while unlocking", e);
        }
    }

    @Override
    public String getDisplayName() {
        if (doc.isFolder()) {
            try {
                return doc.getTitle();
            } catch (ClientException e) {
                return getName();
            }
        } else {
            String fileName = getFileName();
            if (fileName == null) {
                fileName = getName();
            }
            return fileName;
        }
    }

    @Override
    protected String getExtension() {
        String fileName = getFileName();
        if (fileName == null) {
            return super.getExtension();
        } else {
            return new Path(fileName).getFileExtension();
        }
    }

    public void setVirtualName(String virtualName) {
        this.virtualName = virtualName;
    }

    public void setVirtualRootNodeName(String virtualRootNodeName) {
        this.virtualRootNodeName = virtualRootNodeName;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public String getAuthor() {
        try {
            return (String) doc.getPropertyValue("dc:creator");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected String getIconFromType() {
        if ("Workspace".equals(doc.getType())) {
            return "workspace.gif";
        } else {
            return super.getIconFromType();
        }
    }

    @Override
    public boolean isFolderish() {
        return doc.isFolder();
    }

    @Override
    public boolean canCheckOut(String userName) {
        boolean canCheckOut = super.canCheckOut(userName);
        if (canCheckOut) {
            try {
                return getSession().hasPermission(doc.getRef(), "WriteProperties");
            } catch (ClientException e) {
                log.error("Error during permission check", e);
                return false;
            }
        }
        return canCheckOut;
    }

    @Override
    public boolean canUnCheckOut(String userName) {
        boolean canUnCheckOut = super.canUnCheckOut(userName);
        if (canUnCheckOut) {
            try {
                return getSession().hasPermission(doc.getRef(), "WriteProperties");
            } catch (ClientException e) {
                log.error("Error during permission check", e);
                return false;
            }
        }
        return canUnCheckOut;
    }

}
