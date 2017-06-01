/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.sql;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;

@XObject(value = "directory")
public class SQLDirectoryDescriptor extends BaseDirectoryDescriptor {

    public static final int QUERY_SIZE_LIMIT_DEFAULT = 0;

    @XNode("dataSource")
    public String dataSourceName;

    @XNode("querySizeLimit")
    private Integer querySizeLimit;

    @XNodeList(value = "references/tableReference", type = TableReferenceDescriptor[].class, componentType = TableReferenceDescriptor.class)
    private TableReferenceDescriptor[] tableReferences;

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

    public TableReferenceDescriptor[] getTableReferences() {
        return tableReferences;
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
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
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
            clone.tableReferences = new TableReferenceDescriptor[tableReferences.length];
            for (int i = 0; i < tableReferences.length; i++) {
                clone.tableReferences[i] = tableReferences[i].clone();
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
