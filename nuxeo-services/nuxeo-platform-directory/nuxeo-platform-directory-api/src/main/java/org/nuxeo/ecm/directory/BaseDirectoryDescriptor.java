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
 *     Nuxeo - initial API and implementation
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

/**
 * Basic directory descriptor, containing the basic fields used by all directories.
 *
 * @since 8.2
 */
@XObject(value = "directory")
public class BaseDirectoryDescriptor implements Cloneable {

    private static final Log log = LogFactory.getLog(BaseDirectoryDescriptor.class);

    /**
     * How directory semi-"fulltext" searches are matched with a query string.
     * <p>
     * Used for SQL and LDAP directories.
     *
     * @since 8.2
     */
    public enum SubstringMatchType {
        /** Matches initial substring. */
        subinitial,
        /** Matches final substring. */
        subfinal,
        /** Matches any substring. */
        subany
    }

    public static final boolean AUTO_INCREMENT_ID_FIELD_DEFAULT = false;

    public static final int CACHE_TIMEOUT_DEFAULT = 0;

    public static final int CACHE_MAX_SIZE_DEFAULT = 0;

    public static final boolean READ_ONLY_DEFAULT = false;

    public static final SubstringMatchType SUBSTRING_MATCH_TYPE_DEFAULT = SubstringMatchType.subinitial;

    public static final char DEFAULT_DATA_FILE_CHARACTER_SEPARATOR = ',';

    /**
     * Doesn't create or modify the table in any way.
     */
    public static final String CREATE_TABLE_POLICY_NEVER = "never";

    /**
     * Always recreates the table from scratch and loads the CSV data.
     */
    public static final String CREATE_TABLE_POLICY_ALWAYS = "always";

    /**
     * If the table doesn't exist then creates it and loads the CSV data. If the table already exists, only adds missing
     * columns (with null values).
     */
    public static final String CREATE_TABLE_POLICY_ON_MISSING_COLUMNS = "on_missing_columns";

    public static final String CREATE_TABLE_POLICY_DEFAULT = CREATE_TABLE_POLICY_NEVER;

    public static final List<String> CREATE_TABLE_POLICIES = Arrays.asList(CREATE_TABLE_POLICY_NEVER,
            CREATE_TABLE_POLICY_ALWAYS, CREATE_TABLE_POLICY_ON_MISSING_COLUMNS);

    @XNode("@name")
    public String name;

    @XNode("@remove")
    public boolean remove;

    @XNode("@template")
    public boolean template;

    @XNode("@extends")
    public String extendz;

    @XNode("parentDirectory")
    public String parentDirectory;

    @XNode("schema")
    public String schemaName;

    @XNode("idField")
    public String idField;

    @XNode("autoincrementIdField")
    public Boolean autoincrementIdField;

    public boolean isAutoincrementIdField() {
        return autoincrementIdField == null ? AUTO_INCREMENT_ID_FIELD_DEFAULT : autoincrementIdField.booleanValue();
    }

    public void setAutoincrementIdField(boolean autoincrementIdField) {
        this.autoincrementIdField = Boolean.valueOf(autoincrementIdField);
    }

    @XNode("table")
    public String tableName;

    @XNode("readOnly")
    public Boolean readOnly;

    @XNode("passwordField")
    public String passwordField;

    @XNode("passwordHashAlgorithm")
    public String passwordHashAlgorithm;

    @XNodeList(value = "permissions/permission", type = PermissionDescriptor[].class, componentType = PermissionDescriptor.class)
    public PermissionDescriptor[] permissions;

    @XNode("cacheTimeout")
    public Integer cacheTimeout;

    @XNode("cacheMaxSize")
    public Integer cacheMaxSize;

    @XNode("cacheEntryName")
    public String cacheEntryName;

    @XNode("cacheEntryWithoutReferencesName")
    public String cacheEntryWithoutReferencesName;

    @XNode("negativeCaching")
    public Boolean negativeCaching;

    @XNode("substringMatchType")
    public String substringMatchType;

    @XNode("computeMultiTenantId")
    protected boolean computeMultiTenantId = true;

    /**
     * @since 8.4
     */
    @XNodeList(value = "types/type", type = String[].class, componentType = String.class)
    public String[] types;

    /**
     * @since 8.4
     */
    @XNodeList(value = "deleteConstraint", type = ArrayList.class, componentType = DirectoryDeleteConstraintDescriptor.class)
    List<DirectoryDeleteConstraintDescriptor> deleteConstraints;

    /**
     * @since 9.2
     */
    @XNodeList(value = "references/reference", type = ReferenceDescriptor[].class, componentType = ReferenceDescriptor.class)
    ReferenceDescriptor[] references;

    /**
     * @since 9.2
     */
    @XNodeList(value = "references/inverseReference", type = InverseReferenceDescriptor[].class, componentType = InverseReferenceDescriptor.class)
    InverseReferenceDescriptor[] inverseReferences;

    @XNode("dataFile")
    public String dataFileName;

    public String getDataFileName() {
        return dataFileName;
    }

    @XNode(value = "dataFileCharacterSeparator", trim = false)
    public String dataFileCharacterSeparator = ",";

    public char getDataFileCharacterSeparator() {
        char sep;
        if (StringUtils.isEmpty(dataFileCharacterSeparator)) {
            sep = DEFAULT_DATA_FILE_CHARACTER_SEPARATOR;
        } else {
            sep = dataFileCharacterSeparator.charAt(0);
            if (dataFileCharacterSeparator.length() > 1) {
                log.warn("More than one character found for character separator, will use the first one \"" + sep
                        + "\"");
            }
        }
        return sep;
    }

    @XNode("createTablePolicy")
    public String createTablePolicy;

    public String getCreateTablePolicy() {
        if (StringUtils.isBlank(createTablePolicy)) {
            return CREATE_TABLE_POLICY_DEFAULT;
        }
        String ctp = createTablePolicy.toLowerCase();
        if (!CREATE_TABLE_POLICIES.contains(ctp)) {
            throw new DirectoryException("Invalid createTablePolicy: " + createTablePolicy + ", it should be one of: "
                    + CREATE_TABLE_POLICIES);
        }
        return ctp;
    }

    public boolean isReadOnly() {
        return readOnly == null ? READ_ONLY_DEFAULT : readOnly.booleanValue();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = Boolean.valueOf(readOnly);
    }

    public int getCacheTimeout() {
        return cacheTimeout == null ? CACHE_TIMEOUT_DEFAULT : cacheTimeout.intValue();
    }

    public int getCacheMaxSize() {
        return cacheMaxSize == null ? CACHE_MAX_SIZE_DEFAULT : cacheMaxSize.intValue();
    }

    public SubstringMatchType getSubstringMatchType() {
        if (StringUtils.isBlank(substringMatchType)) {
            return SUBSTRING_MATCH_TYPE_DEFAULT;
        }
        try {
            return SubstringMatchType.valueOf(substringMatchType);
        } catch (IllegalArgumentException e) {
            log.error("Unknown value for <substringMatchType>: " + substringMatchType);
            return SUBSTRING_MATCH_TYPE_DEFAULT;
        }
    }

    /**
     * Sub-classes MUST OVERRIDE and use a more specific return type.
     * <p>
     * Usually it's bad to use clone(), and a copy-constructor is preferred, but here we want the copy method to be
     * inheritable so clone() is appropriate.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public BaseDirectoryDescriptor clone() {
        BaseDirectoryDescriptor clone;
        try {
            clone = (BaseDirectoryDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        // basic fields are already copied by super.clone()
        if (permissions != null) {
            clone.permissions = new PermissionDescriptor[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                clone.permissions[i] = permissions[i].clone();
            }
        }
        if (references != null) {
            clone.references = Arrays.stream(references)
                                     .map(ReferenceDescriptor::clone)
                                     .toArray(ReferenceDescriptor[]::new);
        }
        if (inverseReferences != null) {
            clone.inverseReferences = Arrays.stream(inverseReferences).map(InverseReferenceDescriptor::clone).toArray(
                    InverseReferenceDescriptor[]::new);
        }
        return clone;
    }

    public void merge(BaseDirectoryDescriptor other) {
        template = template || other.template;

        if (other.parentDirectory != null) {
            parentDirectory = other.parentDirectory;
        }
        if (other.schemaName != null) {
            schemaName = other.schemaName;
        }
        if (other.idField != null) {
            idField = other.idField;
        }
        if (other.autoincrementIdField != null) {
            autoincrementIdField = other.autoincrementIdField;
        }
        if (other.tableName != null) {
            tableName = other.tableName;
        }
        if (other.readOnly != null) {
            readOnly = other.readOnly;
        }
        if (other.passwordField != null) {
            passwordField = other.passwordField;
        }
        if (other.passwordHashAlgorithm != null) {
            passwordHashAlgorithm = other.passwordHashAlgorithm;
        }
        if (other.permissions != null && other.permissions.length != 0) {
            permissions = other.permissions;
        }
        if (other.cacheTimeout != null) {
            cacheTimeout = other.cacheTimeout;
        }
        if (other.cacheMaxSize != null) {
            cacheMaxSize = other.cacheMaxSize;
        }
        if (other.cacheEntryName != null) {
            cacheEntryName = other.cacheEntryName;
        }
        if (other.cacheEntryWithoutReferencesName != null) {
            cacheEntryWithoutReferencesName = other.cacheEntryWithoutReferencesName;
        }
        if (other.negativeCaching != null) {
            negativeCaching = other.negativeCaching;
        }
        if (other.substringMatchType != null) {
            substringMatchType = other.substringMatchType;
        }
        if (other.types != null) {
            types = other.types;
        }
        if (other.deleteConstraints != null) {
            deleteConstraints = other.deleteConstraints;
        }
        if (other.dataFileName != null) {
            dataFileName = other.dataFileName;
        }
        if (other.dataFileCharacterSeparator != null) {
            dataFileCharacterSeparator = other.dataFileCharacterSeparator;
        }
        if (other.createTablePolicy != null) {
            createTablePolicy = other.createTablePolicy;
        }
        if (other.references != null && other.references.length != 0) {
            references = other.references;
        }
        if (other.inverseReferences != null && other.inverseReferences.length != 0) {
            inverseReferences = other.inverseReferences;
        }
        computeMultiTenantId = other.computeMultiTenantId;
    }

    /**
     * Creates a new {@link Directory} instance from this {@link BaseDirectoryDescriptor).
     */
    public Directory newDirectory() {
        throw new UnsupportedOperationException("Cannot be instantiated as Directory: " + getClass().getName());
    }

    /**
     * @since 8.4
     */
    public List<DirectoryDeleteConstraint> getDeleteConstraints() {
        List<DirectoryDeleteConstraint> res = new ArrayList<>();
        if (deleteConstraints != null) {
            for (DirectoryDeleteConstraintDescriptor deleteConstraintDescriptor : deleteConstraints) {
                res.add(deleteConstraintDescriptor.getDeleteConstraint());
            }
        }
        return res;
    }

    /**
     * @since 9.2
     */
    public ReferenceDescriptor[] getReferences() {
        return references;
    }

    /**
     * @since 9.2
     */
    public InverseReferenceDescriptor[] getInverseReferences() {
        return inverseReferences;
    }

    /**
     * Returns {@code true} if a multi tenant id should be computed for this directory, if the directory has support for
     * multi tenancy, {@code false} otherwise.
     *
     * @since 10.1
     */
    public boolean isComputeMultiTenantId() {
        return computeMultiTenantId;
    }

}
