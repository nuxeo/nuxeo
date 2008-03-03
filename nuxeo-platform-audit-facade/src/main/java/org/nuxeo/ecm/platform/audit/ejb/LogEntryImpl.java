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

package org.nuxeo.ecm.platform.audit.ejb;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryBase;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * LogEntry entity bean implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "listLogEntriesFor", query = "from LogEntryImpl log where log.docUUID=:docUUID ORDER BY log.eventDate DESC"),
        @NamedQuery(name = "allLogEntries", query = "from LogEntryImpl") })
@Table(name = "NXP_LOGS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("NXP")
@EntityListeners(LogEntryCallbackListener.class)
public class LogEntryImpl extends LogEntryBase implements LogEntry {

    private static final long serialVersionUID = 0L;

    public  LogEntryImpl() {
    }

    public LogEntryImpl(DocumentMessage doc) {
        super(doc);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "LOG_ID", nullable = false, columnDefinition = "integer")
    @Override
    public long getId() {
        return id;
    }

    @Column(name = "LOG_PRINCIPAL_NAME")
    @Override
    public String getPrincipalName() {
        return principalName;
    }

    @Column(name = "LOG_EVENT_ID", nullable = false)
    @Override
    public String getEventId() {
        return eventId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LOG_EVENT_DATE")
    @Override
    public Date getEventDate() {
        return eventDate;
    }

    @Column(name = "LOG_DOC_UUID")
    @Override
    public String getDocUUID() {
        return docUUID;
    }

    @Column(name = "LOG_DOC_PATH", length=1024)
    @Override
    public String getDocPath() {
        return docPath;
    }

    @Column(name = "LOG_DOC_TYPE")
    @Override
    public String getDocType() {
        return docType;
    }

    @Column(name = "LOG_EVENT_CATEGORY")
    @Override
    public String getCategory() {
        return category;
    }

    @Column(name = "LOG_EVENT_COMMENT")
    @Override
    public String getComment() {
        return comment;
    }

    @Column(name = "LOG_DOC_LIFE_CYCLE")
    @Override
    public String getDocLifeCycle() {
        return docLifeCycle;
    }

}
