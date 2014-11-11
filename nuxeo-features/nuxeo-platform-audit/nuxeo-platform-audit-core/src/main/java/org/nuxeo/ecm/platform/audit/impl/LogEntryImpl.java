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
 *     Nuxeo - initial API and implementation
 *
 * $Id: LogEntryImpl.java 30195 2008-02-14 21:53:04Z tdelprat $
 */

package org.nuxeo.ecm.platform.audit.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Log entry.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity(name = "LogEntry")
@NamedQueries( {
        @NamedQuery(name = "LogEntry.removeByEventIdAndPath",
                query = "delete LogEntry log where log.eventId = :eventId and log.docPath like :pathPattern"),
        @NamedQuery(name = "LogEntry.findByDocument",
                query = "from LogEntry log where log.docUUID=:docUUID ORDER BY log.eventDate DESC"),
        @NamedQuery(name = "LogEntry.findAll",
                query = "from LogEntry log order by log.eventDate DESC"),
        @NamedQuery(name = "LogEntry.findByEventIdAndPath",
                query = "from LogEntry log where log.eventId=:eventId and log.docPath LIKE :pathPattern"),
        @NamedQuery(name = "LogEntry.findByHavingExtendedInfo",
                query = "from LogEntry log where log.extendedInfos['one'] is not null order by log.eventDate DESC"),
        @NamedQuery(name = "LogEntry.countEventsById",
                query = "select count(log.eventId) from LogEntry log where log.eventId=:eventId"),
        @NamedQuery(name = "LogEntry.findEventIds",
                query = "select distinct log.eventId from LogEntry log")
        })
        @Table(name = "NXP_LOGS")
public class LogEntryImpl implements LogEntry {

    private static final long serialVersionUID = 3037187381843636097L;

    private long id;

    private String principalName;

    private String eventId;

    private Date eventDate;

    private String docUUID;

    private String docType;

    private String docPath;

    private String category;

    private String comment;

    private String docLifeCycle;

    private Map<String, ExtendedInfoImpl> extendedInfos = new HashMap<String, ExtendedInfoImpl>();

    /**
     * @return the log identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "LOG_ID", nullable = false, columnDefinition = "integer")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    @Column(name = "LOG_PRINCIPAL_NAME")
    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    @Column(name = "LOG_EVENT_ID", nullable = false)
    @MapKey(name = "logKey")
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LOG_EVENT_DATE")
    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc UUID related to the log entry.
     */
    @Column(name = "LOG_DOC_UUID")
    public String getDocUUID() {
        return docUUID;
    }

    public void setDocUUID(String docUUID) {
        this.docUUID = docUUID;
    }

    public void setDocUUID(DocumentRef docRef) {
        if (docRef.type() != DocumentRef.ID) {
            throw new IllegalArgumentException("not an id reference " + docRef);
        }
        docUUID = (String) docRef.reference();
    }

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc path related to the log entry.
     */
    @Column(name = "LOG_DOC_PATH", length = 1024)
    public String getDocPath() {
        return docPath;
    }

    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to
     * any document.
     *
     * @return the doc type related to the log entry.
     */
    @Column(name = "LOG_DOC_TYPE")
    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any
     * ways.
     *
     * @return the category for this log entry.
     */
    @Column(name = "LOG_EVENT_CATEGORY")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    @Column(name = "LOG_EVENT_COMMENT", length = 1024)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    @Column(name = "LOG_DOC_LIFE_CYCLE")
    public String getDocLifeCycle() {
        return docLifeCycle;
    }

    public void setDocLifeCycle(String docLifeCycle) {
        this.docLifeCycle = docLifeCycle;
    }

//    public Map<String, ExtendedInfoImpl> getExtendedInfosImpl() {
//        return extendedInfos;
//    }
//
//    public void setExtendedInfosImpl(Map<String, ExtendedInfoImpl> infos) {
//        extendedInfos = infos;
//    }

    @OneToMany(cascade = CascadeType.ALL, targetEntity = ExtendedInfoImpl.class)
    @JoinTable(name = "NXP_LOGS_MAPEXTINFOS", joinColumns = { @JoinColumn(name = "LOG_FK") },
            inverseJoinColumns = { @JoinColumn(name = "INFO_FK") })
    @org.hibernate.annotations.MapKey(columns={@Column(name="mapkey",nullable=false)})
    public Map<String, ExtendedInfo> getExtendedInfos() {
        return (Map)extendedInfos;
        //return (Map)getExtendedInfosImpl();
    }

    public void setExtendedInfos(Map<String, ExtendedInfo> infos) {
        extendedInfos = (Map)infos;
        //setExtendedInfosImpl((Map)infos);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
