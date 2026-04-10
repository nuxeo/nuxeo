/*
 * (C) Copyright 2006-2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Julien Anguenot
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.audit.impl;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.UIAuditComment;
import org.nuxeo.ecm.platform.audit.io.ExtendedInfoDeserializer;
import org.nuxeo.ecm.platform.audit.io.ExtendedInfoSerializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Log entry implementation.
 *
 * @deprecated since 2025.0, use {@link org.nuxeo.audit.api.LogEntry#builder()} instead
 */
@SuppressWarnings("removal")
@Entity(name = "LogEntry")
@NamedQuery(name = "LogEntry.removeByEventIdAndPath", query = "delete LogEntry log where log.eventId = :eventId and log.docPath like :pathPattern")
@NamedQuery(name = "LogEntry.findByDocument", query = "from LogEntry log where log.docUUID=:docUUID ORDER BY log.eventDate DESC")
@NamedQuery(name = "LogEntry.findByDocumentAndRepository", query = "from LogEntry log where log.docUUID=:docUUID and log.repositoryId=:repositoryId ORDER BY log.eventDate DESC")
@NamedQuery(name = "LogEntry.findAll", query = "from LogEntry log order by log.eventDate DESC")
@NamedQuery(name = "LogEntry.findByEventIdAndPath", query = "from LogEntry log where log.eventId=:eventId and log.docPath LIKE :pathPattern")
@NamedQuery(name = "LogEntry.findByHavingExtendedInfo", query = "from LogEntry log where log.extendedInfos['one'] is not null order by log.eventDate DESC")
@NamedQuery(name = "LogEntry.countEventsById", query = "select count(log.eventId) from LogEntry log where log.eventId=:eventId")
@NamedQuery(name = "LogEntry.findEventIds", query = "select distinct log.eventId from LogEntry log")
@Table(name = "NXP_LOGS")
@Deprecated(since = "2025.0", forRemoval = true)
public class LogEntryImpl implements LogEntry {

    @JsonProperty("entity-type")
    protected String entityType;

    private static final long serialVersionUID = 3037187381843636097L;

    private long id;

    /**
     * @since 2025.19 - holds the caller-assigned id while {@link #id} is reset to 0 so Hibernate treats the entity as
     *        transient
     */
    private Long originalId;

    private String principalName;

    private String eventId;

    private Date eventDate;

    private Date logDate;

    private String docUUID;

    private String docType;

    private String docPath;

    private String category;

    private String comment;

    private String docLifeCycle;

    private String repositoryId;

    protected transient UIAuditComment uiComment;

    private Map<String, ExtendedInfo> extendedInfos = new HashMap<>();

    /**
     * @return the log identifier
     */
    @Override
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "nuxeo-log-generator")
    @GenericGenerator(name = "nuxeo-log-generator", type = LogEntrySequenceGenerator.class)
    @Column(name = "LOG_ID", nullable = false, columnDefinition = "integer")
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Introduced to use Nuxeo Sequencer within Hibernate.
     * 
     * @since 2025.19
     */
    @Transient
    public Long getOriginalId() {
        return originalId;
    }

    /**
     * Introduced to use Nuxeo Sequencer within Hibernate.
     * 
     * @since 2025.19
     */
    public void setOriginalId(Long originalId) {
        this.originalId = originalId;
    }

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    @Override
    @Column(name = "LOG_PRINCIPAL_NAME")
    public String getPrincipalName() {
        return principalName;
    }

    @Override
    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    @Override
    @Column(name = "LOG_EVENT_ID", nullable = false)
    @MapKey(name = "logKey")
    public String getEventId() {
        return eventId;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = DateSerializer.class)
    @Column(name = "LOG_EVENT_DATE")
    public Date getEventDate() {
        return eventDate;
    }

    @Override
    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * @return the date of the log insertion: this up to max transaction timeout later than eventDate. This date is
     *         useful for services such as Nuxeo Drive that need fine grained incremental near-monotonic access to the
     *         audit log.
     * @since 5.7
     * @since 5.6-HF16
     */
    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = DateSerializer.class)
    @Column(name = "LOG_DATE")
    public Date getLogDate() {
        return logDate;
    }

    @Override
    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc UUID related to the log entry.
     */
    @Override
    @Column(name = "LOG_DOC_UUID")
    public String getDocUUID() {
        return docUUID;
    }

    @Override
    public void setDocUUID(String docUUID) {
        this.docUUID = docUUID;
    }

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the doc path related to the log entry.
     */
    @Override
    @Column(name = "LOG_DOC_PATH", length = 1024)
    public String getDocPath() {
        return docPath;
    }

    @Override
    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to any document.
     *
     * @return the doc type related to the log entry.
     */
    @Override
    @Column(name = "LOG_DOC_TYPE")
    public String getDocType() {
        return docType;
    }

    @Override
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any ways.
     *
     * @return the category for this log entry.
     */
    @Override
    @Column(name = "LOG_EVENT_CATEGORY")
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    @Override
    @Column(name = "LOG_EVENT_COMMENT", length = 1024)
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    @Override
    @Column(name = "LOG_DOC_LIFE_CYCLE")
    public String getDocLifeCycle() {
        return docLifeCycle;
    }

    @Override
    public void setDocLifeCycle(String docLifeCycle) {
        this.docLifeCycle = docLifeCycle;
    }

    /**
     * Returns the repository id related to the log entry.
     *
     * @return the repository id
     */
    @Override
    @Column(name = "LOG_REPO_ID")
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Transient
    @Override
    public Map<String, Object> getExtended() {
        var extended = new HashMap<String, Object>();
        for (var entry : extendedInfos.entrySet()) {
            extended.put(entry.getKey(), entry.getValue().getSerializableValue());
        }
        return extended;
    }

    @Override
    @JsonProperty("extended")
    @JsonSerialize(keyAs = String.class, contentUsing = ExtendedInfoSerializer.class)
    @OneToMany(cascade = CascadeType.ALL, targetEntity = ExtendedInfoImpl.class)
    @JoinTable(name = "NXP_LOGS_MAPEXTINFOS", joinColumns = { @JoinColumn(name = "LOG_FK") }, inverseJoinColumns = {
            @JoinColumn(name = "INFO_FK") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                    "INFO_FK" }, name = "nxp_logs_mapextinfos_info_fk_key"))
    @MapKeyColumn(name = "mapkey", nullable = false)
    public Map<String, ExtendedInfo> getExtendedInfos() {
        return extendedInfos;
    }

    @Override
    @JsonDeserialize(keyAs = String.class, contentUsing = ExtendedInfoDeserializer.class)
    public void setExtendedInfos(Map<String, ExtendedInfo> infos) {
        extendedInfos = infos;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Transient
    @Override
    public UIAuditComment getPreprocessedComment() {
        return uiComment;
    }

    @Override
    public void setPreprocessedComment(UIAuditComment uiComment) {
        this.uiComment = uiComment;
    }

    /**
     * Specific date serializer to have compliant dates with both current and 8.10 Elasticsearch mapping
     *
     * @since 9.3
     */
    static class DateSerializer extends JsonSerializer<Date> {

        public DateSerializer() {
            // default constructor
        }

        @Override
        public void serialize(Date date, JsonGenerator jg, SerializerProvider serializers) throws IOException {
            jg.writeObject(date.toInstant().toString());
        }
    }

}
