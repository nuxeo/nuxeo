/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

        private LongInfo(long value) {
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

        private DateInfo(Date value) {
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

        private StringInfo(String value) {
            stringValue = value;
        }

        private String stringValue;

        @Override
        @Transient
        public Serializable getSerializableValue() {
            return stringValue;
        }

        @Column(name = "LOG_EXTINFO_STRING")

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

        private DoubleInfo(Double value) {
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

        private BooleanInfo(Boolean value) {
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

        private BlobInfo(Serializable value) {
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
