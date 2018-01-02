/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.audit.impl;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;

/**
 * Extended audit info entities, used to persist contributed extended information.
 *
 * @author Stephane Lacoin (Nuxeo EP software engineer)
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "NXP_LOGS_EXTINFO")
@DiscriminatorColumn(name = "DISCRIMINATOR")
public class ExtendedInfoImpl implements ExtendedInfo {

    private static final long serialVersionUID = 1L;

    private ExtendedInfoImpl() {
    }

    public static ExtendedInfoImpl createExtendedInfo(Serializable value) {
        Class<?> clazz = value.getClass();
        if (Long.class.isAssignableFrom(clazz)) {
            return new LongInfo((Long) value);
        }
        if (Double.class.isAssignableFrom(clazz)) {
            return new DoubleInfo((Double) value);
        }
        if (Date.class.isAssignableFrom(clazz)) {
            return new DateInfo((Date) value);
        }
        if (String.class.isAssignableFrom(clazz)) {
            return new StringInfo((String) value);
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            return new BooleanInfo((Boolean) value);
        }
        return new BlobInfo(value);
    }

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "LOG_EXTINFO_ID")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Transient
    public Serializable getSerializableValue() {
        throw new UnsupportedOperationException();
    }

    public <T> T getValue(Class<T> clazz) {
        return clazz.cast(this.getSerializableValue());
    }

    @Entity
    @DiscriminatorValue(value = "LONG")
    public static class LongInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private LongInfo() {
        }

        public LongInfo(long value) {
            this.longValue = value;
        }

        private long longValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return longValue;
        }

        @Column(name = "LOG_EXTINFO_LONG")
        public Long getLongValue() {
            return longValue;
        }

        public void setLongValue(Long value) {
            this.longValue = value;
        }
    }

    @Entity
    @DiscriminatorValue(value = "DATE")
    public static class DateInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private DateInfo() {
        }

        public DateInfo(Date value) {
            dateValue = value;
        }

        private Date dateValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return dateValue;
        }

        @Column(name = "LOG_EXTINFO_DATE")
        @Temporal(value = TemporalType.TIMESTAMP)
        public Date getDateValue() {
            return dateValue;
        }

        public void setDateValue(Date value) {
            dateValue = value;
        }
    }

    @Entity
    @DiscriminatorValue(value = "STRING")
    public static class StringInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private StringInfo() {
        }

        public StringInfo(String value) {
            stringValue = value;
        }

        private String stringValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return stringValue;
        }

        @Column(name = "LOG_EXTINFO_STRING", length = 1024)
        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String value) {
            stringValue = value;
        }
    }

    @Entity
    @DiscriminatorValue(value = "DOUBLE")
    public static class DoubleInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private DoubleInfo() {
        }

        public DoubleInfo(Double value) {
            doubleValue = value;
        }

        private Double doubleValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return doubleValue;
        }

        @Column(name = "LOG_EXTINFO_DOUBLE")
        public Double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(Double value) {
            doubleValue = value;
        }
    }

    @Entity
    @DiscriminatorValue(value = "BOOLEAN")
    public static class BooleanInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private BooleanInfo() {
        }

        public BooleanInfo(Boolean value) {
            booleanValue = value;
        }

        private Boolean booleanValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return booleanValue;
        }

        @Column(name = "LOG_EXTINFO_BOOLEAN")
        public Boolean getBooleanValue() {
            return booleanValue;
        }

        public void setBooleanValue(Boolean value) {
            booleanValue = value;
        }
    }

    @Entity
    @DiscriminatorValue(value = "BLOB")
    public static class BlobInfo extends ExtendedInfoImpl {

        private static final long serialVersionUID = 1L;

        private BlobInfo() {
        }

        public BlobInfo(Serializable value) {
            blobValue = value;
        }

        private Serializable blobValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return blobValue;
        }

        @Column(name = "LOG_EXTINFO_BLOB")
        @Lob
        public Serializable getBlobValue() {
            return blobValue;
        }

        public void setBlobValue(Serializable value) {
            blobValue = value;
        }

    }

}
