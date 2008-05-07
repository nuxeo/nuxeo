/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.archive.ejb;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;

/**
 * Archive record entity bean implementation.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 *
 */
@Entity
@NamedQueries({ @NamedQuery(name = "listArchiveRecordsByDocUID",
        query = "from ArchiveRecordImpl archive where archive.docUID=:docUID ORDER BY archive.docVersion DESC") })
@Table(name = "document_archives")
public class ArchiveRecordImpl implements ArchiveRecord {

    private static final long serialVersionUID = 7455315727125364279L;

    private static final Log log = LogFactory.getLog(ArchiveRecordImpl.class);

    private long id;

    protected String docUID;

    private String docVersion;

    private String docLifeCycle;

    private String docTitle;

    private String parentDocPath;

    private String docType;

    private String retentionMediumState;

    private String retentionMediumQuality;

    private String retentionMediumType;

    private String retentionMediumLocation;

    private Date archiveDate;

    private String format1;

    private Integer folios1;

    private Integer microformNumber;

    private String restMediumState;

    private String restMediumType;

    private String restMediumLoc;

    private String writableMediumLoc;

    private String writableMediumType;

    private String writableMediumState;

    private Integer retentionMediumMaxAge;

    private Date retentionMediumLastYear;

    private Date estimatedRemovalDate;

    private String gatheringFolder;

    public ArchiveRecordImpl() {
    }

    public ArchiveRecordImpl(DocumentModel currentDocument) {
        try {
            setDocUID(currentDocument.getId());
            setDocType(currentDocument.getType());
            setDocLifeCycle(currentDocument.getCurrentLifeCycleState());
            setDocTitle((String) currentDocument.getProperty("dublincore",
                    "title"));
            setParentDocPath(currentDocument.getPath().removeLastSegments(1).toString());
        } catch (ClientException e) {
            log.debug(
                    "there was an error while generating the archive record from the current document ",
                    e);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, columnDefinition = "integer")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "archive_date")
    public Date getArchiveDate() {
        return archiveDate;
    }

    public void setArchiveDate(Date archiveDate) {
        this.archiveDate = archiveDate;
    }

    @Column(name = "folio1", length = 4)
    public Integer getFolios1() {
        return folios1;
    }

    public void setFolios1(Integer folios1) {
        this.folios1 = folios1;
    }

    @Column(name = "format1")
    public String getFormat1() {
        return format1;
    }

    public void setFormat1(String format1) {
        this.format1 = format1;
    }

    @Column(name = "nb_microform", columnDefinition = "integer")
    public Integer getMicroformNumber() {
        return microformNumber;
    }

    public void setMicroformNumber(Integer microformNumber) {
        this.microformNumber = microformNumber;
    }

    @Column(name = "rest_medium_location")
    public String getRestMediumLoc() {
        return restMediumLoc;
    }

    public void setRestMediumLoc(String restMediumLoc) {
        this.restMediumLoc = restMediumLoc;
    }

    @Column(name = "rest_medium_state")
    public String getRestMediumState() {
        return restMediumState;
    }

    public void setRestMediumState(String restMediumState) {
        this.restMediumState = restMediumState;
    }

    @Column(name = "writable_medium_location")
    public String getWritableMediumLoc() {
        return writableMediumLoc;
    }

    public void setWritableMediumLoc(String writableMediumLoc) {
        this.writableMediumLoc = writableMediumLoc;
    }

    @Column(name = "lc_state", length = 16)
    public String getDocLifeCycle() {
        return docLifeCycle;
    }

    public void setDocLifeCycle(String docLifeCycle) {
        this.docLifeCycle = docLifeCycle;
    }

    @Column(name = "parent_path")
    public String getParentDocPath() {
        return parentDocPath;
    }

    public void setParentDocPath(String docPath) {
        this.parentDocPath = docPath;
    }

    @Column(name = "title")
    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    @Column(name = "type")
    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    @Column(name = "doc_UID")
    public String getDocUID() {
        return docUID;
    }

    public void setDocUID(String docUID) {
        this.docUID = docUID;
    }

    @Column(name = "doc_version", length = 5)
    public String getDocVersion() {
        return docVersion;
    }

    public void setDocVersion(String docVersion) {
        this.docVersion = docVersion;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "estimated_removal_date")
    public Date getEstimatedRemovalDate() {
        return estimatedRemovalDate;
    }

    public void setEstimatedRemovalDate(Date estimatedRemovalDate) {
        this.estimatedRemovalDate = estimatedRemovalDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "retention_medium_lastyear")
    public Date getRetentionMediumLastYear() {
        return retentionMediumLastYear;
    }

    public void setRetentionMediumLastYear(Date retentionMediumLastYear) {
        this.retentionMediumLastYear = retentionMediumLastYear;
    }

    @Column(name = "retention_medium_location")
    public String getRetentionMediumLocation() {
        return retentionMediumLocation;
    }

    public void setRetentionMediumLocation(String retentionMediumLocation) {
        this.retentionMediumLocation = retentionMediumLocation;
    }

    @Column(name = "retention_medium_maxage", columnDefinition = "integer")
    public Integer getRetentionMediumMaxAge() {
        return retentionMediumMaxAge;
    }

    public void setRetentionMediumMaxAge(Integer retentionMediumMaxAge) {
        this.retentionMediumMaxAge = retentionMediumMaxAge;
    }

    @Column(name = "retention_medium_quality", length = 10)
    public String getRetentionMediumQuality() {
        return retentionMediumQuality;
    }

    public void setRetentionMediumQuality(String retentionMediumQuality) {
        this.retentionMediumQuality = retentionMediumQuality;
    }

    @Column(name = "retention_medium_state")
    public String getRetentionMediumState() {
        return retentionMediumState;
    }

    public void setRetentionMediumState(String retentionMediumState) {
        this.retentionMediumState = retentionMediumState;
    }

    @Column(name = "retention_medium_type")
    public String getRetentionMediumType() {
        return retentionMediumType;
    }

    public void setRetentionMediumType(String retentionMediumType) {
        this.retentionMediumType = retentionMediumType;
    }

    @Column(name = "writable_medium_state")
    public String getWritableMediumState() {
        return writableMediumState;
    }

    public void setWritableMediumState(String writableMediumState) {
        this.writableMediumState = writableMediumState;
    }

    @Column(name = "writable_medium_type")
    public String getWritableMediumType() {
        return writableMediumType;
    }

    public void setWritableMediumType(String writableMediumType) {
        this.writableMediumType = writableMediumType;
    }

    @Column(name = "gathering_folder", columnDefinition = "char(1)")
    public String getGatheringFolder() {
        return gatheringFolder;
    }

    /**
     * @param gatheringFolder The gatheringFolder to set.
     */
    public void setGatheringFolder(String gatheringFolder) {
        this.gatheringFolder = gatheringFolder;
    }

    @Column(name = "rest_medium_type")
    public String getRestMediumType() {
        return restMediumType;
    }

    /**
     * @param restMediumType The restMediumType to set.
     */
    public void setRestMediumType(String restMediumType) {
        this.restMediumType = restMediumType;
    }
}
