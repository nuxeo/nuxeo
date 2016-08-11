/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.Reference;

@XObject(value = "directory")
public class SQLDirectoryDescriptor extends BaseDirectoryDescriptor {

    private static final Log log = LogFactory.getLog(SQLDirectoryDescriptor.class);

    public static final int QUERY_SIZE_LIMIT_DEFAULT = 0;

    public static final boolean AUTO_INCREMENT_ID_FIELD_DEFAULT = false;

    public static final char DEFAULT_CHARACTER_SEPARATOR = ',';

    public static final String[] CREATE_TABLE_POLICIES = { "never", "on_missing_columns", "always", };

    public static final String CREATE_TABLE_POLICY_DEFAULT = "never";


    @XNode("dataSource")
    public String dataSourceName;

    @XNode("dataFile")
    public String dataFileName;

    @XNode(value = "dataFileCharacterSeparator", trim = false)
    public String dataFileCharacterSeparator = ",";

    public String createTablePolicy;

    @XNode("autoincrementIdField")
    public Boolean autoincrementIdField;

    @XNode("querySizeLimit")
    private Integer querySizeLimit;

    @XNodeList(value = "references/tableReference", type = TableReference[].class, componentType = TableReference.class)
    private TableReference[] tableReferences;

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNodeList(value = "filters/staticFilter", type = SQLStaticFilter[].class, componentType = SQLStaticFilter.class)
    private SQLStaticFilter[] staticFilters;

    @XNode("nativeCase")
    public Boolean nativeCase;

    @XNode("computeMultiTenantId")
    private boolean computeMultiTenantId = true;

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public char getDataFileCharacterSeparator() {
        if (dataFileCharacterSeparator == null || dataFileCharacterSeparator.length() == 0) {
            log.info("Character separator not well set will " + "take the default value, \""
                    + DEFAULT_CHARACTER_SEPARATOR + "\"");
            return DEFAULT_CHARACTER_SEPARATOR;
        }

        if (dataFileCharacterSeparator.length() > 1) {
            log.warn("More than one character found for character separator, " + "will take the first one \""
                    + dataFileCharacterSeparator.charAt(0) + "\"");
        }

        return dataFileCharacterSeparator.charAt(0);
    }

    public String getCreateTablePolicy() {
        return createTablePolicy;
    }

    @XNode("createTablePolicy")
    public void setCreateTablePolicy(String createTablePolicy) throws DirectoryException {
        if (createTablePolicy == null) {
            this.createTablePolicy = CREATE_TABLE_POLICY_DEFAULT;
            return;
        }
        createTablePolicy = createTablePolicy.toLowerCase();
        boolean validPolicy = false;
        for (String policy : CREATE_TABLE_POLICIES) {
            if (createTablePolicy.equals(policy)) {
                validPolicy = true;
                break;
            }
        }
        if (!validPolicy) {
            throw new DirectoryException("invalid value for createTablePolicy: " + createTablePolicy
                    + ". It should be one of 'never', " + "'on_missing_columns',  or 'always'.");
        }
        this.createTablePolicy = createTablePolicy;
    }

    public Reference[] getInverseReferences() {
        return inverseReferences;
    }

    public Reference[] getTableReferences() {
        return tableReferences;
    }

    public boolean isAutoincrementIdField() {
        return autoincrementIdField == null ? AUTO_INCREMENT_ID_FIELD_DEFAULT : autoincrementIdField.booleanValue();
    }

    public void setAutoincrementIdField(boolean autoincrementIdField) {
        this.autoincrementIdField = Boolean.valueOf(autoincrementIdField);
    }

    public void setInverseReferences(InverseReference[] inverseReferences) {
        this.inverseReferences = inverseReferences;
    }

    public void setDataFileName(String dataFile) {
        this.dataFileName = dataFile;
    }

    public void setTableReferences(TableReference[] tableReferences) {
        this.tableReferences = tableReferences;
    }

    public int getQuerySizeLimit() {
        return querySizeLimit == null ? QUERY_SIZE_LIMIT_DEFAULT : querySizeLimit.intValue();
    }

    public void setQuerySizeLimit(int querySizeLimit) {
        this.querySizeLimit = Integer.valueOf(querySizeLimit);
    }

    public SQLStaticFilter[] getStaticFilters() {
        if (staticFilters == null) {
            return new SQLStaticFilter[0];
        }
        return staticFilters;
    }

    /**
     * Returns {@code true} if a multi tenant id should be computed for this directory, if the directory has support for
     * multi tenancy, {@code false} otherwise.
     *
     * @since 5.6
     */
    public boolean isComputeMultiTenantId() {
        return computeMultiTenantId;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof SQLDirectoryDescriptor) {
            merge((SQLDirectoryDescriptor) other);
        }
    }

    protected void merge(SQLDirectoryDescriptor other) {
        if (other.dataSourceName != null) {
            dataSourceName = other.dataSourceName;
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
        if (other.autoincrementIdField != null) {
            autoincrementIdField = other.autoincrementIdField;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.inverseReferences != null && other.inverseReferences.length != 0) {
            inverseReferences = other.inverseReferences;
        }
        if (other.tableReferences != null && other.tableReferences.length != 0) {
            tableReferences = other.tableReferences;
        }
        if (other.staticFilters != null && other.staticFilters.length != 0) {
            staticFilters = other.staticFilters;
        }
        if (other.nativeCase != null) {
            nativeCase = other.nativeCase;
        }
        computeMultiTenantId = other.computeMultiTenantId;
    }

    @Override
    public SQLDirectoryDescriptor clone() {
        SQLDirectoryDescriptor clone = (SQLDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        if (tableReferences != null) {
            clone.tableReferences = new TableReference[tableReferences.length];
            for (int i = 0; i < tableReferences.length; i++) {
                clone.tableReferences[i] = tableReferences[i].clone();
            }
        }
        if (inverseReferences != null) {
            clone.inverseReferences = new InverseReference[inverseReferences.length];
            for (int i = 0; i < inverseReferences.length; i++) {
                clone.inverseReferences[i] = inverseReferences[i].clone();
            }
        }
        if (staticFilters != null) {
            clone.staticFilters = new SQLStaticFilter[staticFilters.length];
            for (int i = 0; i < staticFilters.length; i++) {
                clone.staticFilters[i] = staticFilters[i].clone();
            }
        }
        return clone;
    }

    @Override
    public SQLDirectory newDirectory() {
        return new SQLDirectory(this);
    }

}
