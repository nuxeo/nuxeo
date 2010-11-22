/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     ${user}
 *
 * $$Id: SummaryEntry.java 28482 2008-01-04 15:33:39Z sfermigier $$
 */

/**
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 *
 */
package org.nuxeo.ecm.webapp.clipboard;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * An entry present in a Summary. Each entry has a parent,
 * except for the root entry whose parent is null.
 * <p>
 * Note that {@code SummaryEntry.getPath()} can be different than
 * {@code DocumentModel.getPath()}
 * since a DocumentModel object can be added many times at different level of the
 * working list.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public class SummaryEntry implements Comparable<SummaryEntry>, Serializable {

    private static final long serialVersionUID = -9090607163794413025L;

    private static final DateFormat DATE_PARSER = new SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss");

    private String marker = " ";

    private String bullet = "* ";

    private String pathSeparator = "/";

    private String fileSeparator = " -> ";

    private String versionSepartor = "_";

    private String dateSeparator = " - ";

    private String cr = "\n";

    private String uuid;

    private DocumentRef documentRef;

    private String title;

    private String modifiedDate;

    private String filename;

    private String version;

    private SummaryEntry parent;

    // Not used?
    public SummaryEntry(String uuid, String title, String modifiedDate,
            String filename, String version) {
        this.uuid = uuid;
        this.title = title;
        this.modifiedDate = modifiedDate;
        this.filename = filename;
        this.version = version;
    }

    // Used in ClipBoardActionBean
    public SummaryEntry(String uuid, String title, Date modifiedDate,
            String filename, String version, SummaryEntry parent) {
        this.uuid = uuid;
        this.title = title;
        if (modifiedDate != null) {
            this.modifiedDate = DATE_PARSER.format(modifiedDate);
        }
        this.filename = filename;
        this.version = version;
        this.parent = parent;
    }

    // Used in ClipBoardActionBean
    public SummaryEntry(DocumentModel doc) {
        uuid = doc.getRef().toString();
        try {
            title = (String) doc.getProperty("dublincore", "title");
        } catch (ClientException e) {
            title = null;
        }
        documentRef = doc.getRef();

        Object major;
        try {
            major = doc.getProperty("uid", "major_version");
        } catch (ClientException e) {
            major = null;
        }
        Object minor;
        try {
            minor = doc.getProperty("uid", "minor_version");
        } catch (ClientException e) {
            minor = null;
        }
        Object date;
        try {
            date = doc.getProperty("dublincore", "modified");
        } catch (ClientException e) {
            date = null;
        }

        if (major != null && minor != null) {
            version = major.toString() + '.' + minor.toString();
        }

        if (date != null) {
            modifiedDate = DATE_PARSER.format(((Calendar) date).getTime());
        }
        try {
            filename = (String) doc.getProperty("file", "filename");
        } catch (ClientException e) {
            filename = null;
        }
    }

    public SummaryEntry(DocumentRef reference) {
        documentRef = reference;
    }

    public SummaryEntry() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the entry for a flat view
     */
    public String toFlatString() {
        StringBuilder sb = new StringBuilder();

        sb.append(cr);
        sb.append(marker);

        sb.append(getPath());

        if (filename != null && !"".equals(filename)) {
            sb.append(fileSeparator);
            sb.append(filename);
        }
        if (version != null && !"".equals(version)) {
            sb.append(versionSepartor);
            sb.append(version);
        }
        if (modifiedDate != null && !"".equals(modifiedDate)) {
            sb.append(dateSeparator);
            sb.append(modifiedDate);
        }

        return sb.toString();
    }

    /**
     * @return the entry for a hierarchical view
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();

        sb.append(cr);
        sb.append(marker);
        sb.append(bullet);
        sb.append(title);

        if (filename != null && !"".equals(filename)) {
            sb.append(fileSeparator);
            sb.append(filename);
        }
        if (version != null && !"".equals(version)) {
            sb.append(versionSepartor);
            sb.append(version);
        }
        if (modifiedDate != null && !"".equals(modifiedDate)) {
            sb.append(dateSeparator);
            sb.append(modifiedDate);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toFlatString();
    }

    public SummaryEntry getParent() {
        return parent;
    }

    public void setParent(SummaryEntry parent) {
        this.parent = parent;
    }

    public void setParent(DocumentRef parentRef) {
        parent = new SummaryEntry(parentRef);
    }

    public DocumentRef getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(DocumentRef documentRef) {
        this.documentRef = documentRef;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SummaryEntry)) {
            return false;
        }
        return documentRef.equals(((SummaryEntry) obj).documentRef)
                && getPath().equals(((SummaryEntry) obj).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    public boolean hasParent() {
        return parent != null;
    }

    /**
     * Calls itself recursively to build a list with all entry of the path.
     *
     * @param pathList the list where to add the found path item
     * @param parentEntry is the SummaryEntry to watch
     * @return all entry in the path for a given entry (parentEntry).
     */
    public static List<SummaryEntry> getPathList(List<SummaryEntry> pathList,
            SummaryEntry parentEntry) {
        if (pathList == null) {
            pathList = new ArrayList<SummaryEntry>();
        }
        if (parentEntry != null) {
            pathList.add(parentEntry);
            if (parentEntry.parent != null) {
                getPathList(pathList, parentEntry.parent);
            }
        }

        return pathList;
    }

    /**
     * Returns something like /Workspace1/Folder1/File1.
     * Is not tied to DocumentModel.getPath()
     *
     * @return the full path of the SummaryEntry in the summary.
     */
    public String getPath() {
        StringBuilder sb = new StringBuilder();
        List<SummaryEntry> pathList = getPathList(null, this);
        Collections.reverse(pathList);
        int i = 0;
        for (SummaryEntry entry : pathList) {
            sb.append(entry.title);
            if (i < pathList.size() - 1) {
                sb.append('/');
            }
            i++;
        }

        return sb.toString();
    }

    public int compareTo(SummaryEntry o) {
        if (o != null) {
            return getPath().compareTo(o.getPath());
        }
        return 0;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getBullet() {
        return bullet;
    }

    public void setBullet(String bullet) {
        this.bullet = bullet;
    }
    public String getCr() {
        return cr;
    }

    public void setCr(String cr) {
        this.cr = cr;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    public String getFileSeparator() {
        return fileSeparator;
    }

    public void setFileSeparator(String fileSeparator) {
        this.fileSeparator = fileSeparator;
    }

    public String getVersionSepartor() {
        return versionSepartor;
    }

    public void setVersionSepartor(String versionSepartor) {
        this.versionSepartor = versionSepartor;
    }

    public String getDateSeparator() {
        return dateSeparator;
    }

    public void setDateSeparator(String dateSeparator) {
        this.dateSeparator = dateSeparator;
    }

}
