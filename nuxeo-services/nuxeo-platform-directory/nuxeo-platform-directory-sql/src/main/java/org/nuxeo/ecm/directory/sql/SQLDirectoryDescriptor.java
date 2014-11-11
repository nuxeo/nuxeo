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
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.Reference;

@XObject(value = "directory")
public class SQLDirectoryDescriptor {

    private static final Log log = LogFactory.getLog(SQLDirectoryDescriptor.class);

    public enum SubstringMatchType {
        subinitial, subfinal, subany
    }

    protected static final char DEFAULT_CHARACTER_SEPARATOR = ',';

    private static final String[] SCRIPT_POLICIES = { "never",
            "on_missing_columns", "always", };

    private static final String DEFAULT_POLICY = "never";

    @XNode("@name")
    public String name;

    @XNode("schema")
    public String schemaName;

    @XNode("parentDirectory")
    public String parentDirectory;

    @XNode("dataSource")
    public String dataSourceName;

    @XNode("dbDriver")
    public String dbDriver;

    @XNode("dbUrl")
    public String dbUrl;

    @XNode("dbUser")
    public String dbUser;

    @XNode("dbPassword")
    public String dbPassword;

    @XNode("table")
    public String tableName;

    @XNodeList(value = "init-dependencies/dependency", type = ArrayList.class, componentType = String.class)
    public List<String> initDependencies;

    @XNode("idField")
    public String idField;

    @XNode("dataFile")
    public String dataFileName;

    @XNode(value = "dataFileCharacterSeparator", trim = false)
    public String dataFileCharacterSeparator = ",";

    public String createTablePolicy;

    public SubstringMatchType substringMatchType;

    @XNode("autoincrementIdField")
    public boolean autoincrementIdField;

    @XNode("readOnly")
    public Boolean readOnly = Boolean.FALSE;

    @XNode("passwordField")
    private String passwordField;

    @XNode("passwordHashAlgorithm")
    public String passwordHashAlgorithm;

    @XNode("querySizeLimit")
    private int querySizeLimit;

    @XNodeList(value = "references/tableReference", type = TableReference[].class, componentType = TableReference.class)
    private TableReference[] tableReferences;

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNode("@remove")
    private boolean remove = false;

    @XNode("cacheTimeout")
    public int cacheTimeout = 0;

    @XNode("cacheMaxSize")
    public int cacheMaxSize = 0;

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

    public void setIdField(String idField) {
        this.idField = idField;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    // XXX never used: is it supposed to help determining an entry full id
    // using
    // the parent directory id?
    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public char getDataFileCharacterSeparator() {
        if (dataFileCharacterSeparator == null
                || dataFileCharacterSeparator.length() == 0) {
            log.info("Character separator not well set will "
                    + "take the default value, \""
                    + DEFAULT_CHARACTER_SEPARATOR + "\"");
            return DEFAULT_CHARACTER_SEPARATOR;
        }

        if (dataFileCharacterSeparator.length() > 1) {
            log.warn("More than one character found for character separator, "
                    + "will take the first one \""
                    + dataFileCharacterSeparator.charAt(0) + "\"");
        }

        return dataFileCharacterSeparator.charAt(0);
    }

    public String getPasswordField() {
        return passwordField;
    }

    public void setPasswordField(String passwordField) {
        this.passwordField = passwordField;
    }

    public String getIdField() {
        return idField;
    }

    public String getCreateTablePolicy() {
        return createTablePolicy;
    }

    @XNode("createTablePolicy")
    public void setCreateTablePolicy(String createTablePolicy)
            throws DirectoryException {
        if (createTablePolicy == null) {
            this.createTablePolicy = DEFAULT_POLICY;
            return;
        }
        createTablePolicy = createTablePolicy.toLowerCase();
        boolean validPolicy = false;
        for (String policy : SCRIPT_POLICIES) {
            if (createTablePolicy.equals(policy)) {
                validPolicy = true;
                break;
            }
        }
        if (!validPolicy) {
            throw new DirectoryException(
                    "invalid value for createTablePolicy: " + createTablePolicy
                            + ". It should be one of 'never', "
                            + "'on_missing_columns',  or 'always'.");
        }
        this.createTablePolicy = createTablePolicy;
    }

    @XNode("substringMatchType")
    public void setSubstringMatchType(String substringMatchType) {
        if (substringMatchType != null) {
            try {
                this.substringMatchType = Enum.valueOf(
                        SubstringMatchType.class, substringMatchType);
            } catch (IllegalArgumentException iae) {
                log.error("Invalid substring match type: " + substringMatchType
                        + ". Valid options: subinitial, subfinal, subany");
                this.substringMatchType = SubstringMatchType.subinitial;
            }
        }
    }

    public Reference[] getInverseReferences() {
        return inverseReferences;
    }

    public Reference[] getTableReferences() {
        return tableReferences;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isAutoincrementIdField() {
        return autoincrementIdField;
    }

    public void setAutoincrementIdField(boolean autoincrementIdField) {
        this.autoincrementIdField = autoincrementIdField;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
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
        return querySizeLimit;
    }

    public void setQuerySizeLimit(int querySizeLimit) {
        this.querySizeLimit = querySizeLimit;
    }

    public void setRemove(boolean delete) {
        this.remove = delete;
    }

    public boolean getRemove() {
        return this.remove;
    }

    public int getCacheTimeout() {
        return cacheTimeout;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public SubstringMatchType getSubstringMatchType() {
        return substringMatchType == null ? SubstringMatchType.subinitial
                : substringMatchType;
    }

    public void setSubstringMatchType(SubstringMatchType substringMatchType) {
        this.substringMatchType = substringMatchType;
    }

    public SQLStaticFilter[] getStaticFilters() {
        if (staticFilters == null) {
            return new SQLStaticFilter[0];
        }
        return staticFilters;
    }

    /**
     * Returns {@code true} if a multi tenant id should be computed for this
     * directory, if the directory has support for multi tenancy, {@code false}
     * otherwise.
     *
     * @since 5.6
     */
    public boolean isComputeMultiTenantId() {
        return computeMultiTenantId;
    }

    /**
     * Merge re-written since 5.6 to comply to hot reload needs, omitting to
     * merge properties initialized by xmap)
     */
    public void merge(SQLDirectoryDescriptor other) {
        merge(other, false);
    }

    public void merge(SQLDirectoryDescriptor other, boolean overwite) {
        if (other.dataSourceName != null || overwite) {
            dataSourceName = other.dataSourceName;
        }
        if (other.dbDriver != null || overwite) {
            dbDriver = other.dbDriver;
        }
        if (other.dbUrl != null || overwite) {
            dbUrl = other.dbUrl;
        }
        if (other.dbUser != null || overwite) {
            dbUser = other.dbUser;
        }
        if (other.dbPassword != null || overwite) {
            dbPassword = other.dbPassword;
        }
        if (other.tableName != null || overwite) {
            tableName = other.tableName;
        }
        if (other.schemaName != null || overwite) {
            schemaName = other.schemaName;
        }
        if (other.parentDirectory != null || overwite) {
            parentDirectory = other.parentDirectory;
        }
        if ((other.initDependencies != null && other.initDependencies.size() != 0)
                || overwite) {
            initDependencies = other.initDependencies;
        }
        if (other.idField != null || overwite) {
            idField = other.idField;
        }
        if (other.dataFileName != null || overwite) {
            dataFileName = other.dataFileName;
        }
        if (other.dataFileCharacterSeparator != null || overwite) {
            dataFileCharacterSeparator = other.dataFileCharacterSeparator;
        }
        if (other.createTablePolicy != null || overwite) {
            createTablePolicy = other.createTablePolicy;
        }
        if (other.substringMatchType != null || overwite) {
            substringMatchType = other.substringMatchType;
        }
        if (overwite) {
            autoincrementIdField = other.autoincrementIdField;
        }
        if (other.readOnly != null || overwite) {
            readOnly = other.readOnly;
        }
        if (other.passwordField != null || overwite) {
            passwordField = other.passwordField;
        }
        if (other.passwordHashAlgorithm != null || overwite) {
            passwordHashAlgorithm = other.passwordHashAlgorithm;
        }
        if (overwite) {
            querySizeLimit = other.querySizeLimit;
        }

        if ((other.inverseReferences != null && other.inverseReferences.length != 0)
                || overwite) {
            inverseReferences = other.inverseReferences;
        }
        if ((other.tableReferences != null && other.tableReferences.length != 0)
                || overwite) {
            tableReferences = other.tableReferences;
        }

        remove = other.remove;

        if (overwite) {
            cacheTimeout = other.cacheTimeout;
            cacheMaxSize = other.cacheMaxSize;
        }
        if ((other.staticFilters != null && other.staticFilters.length != 0)
                || overwite) {
            staticFilters = other.staticFilters;
        }
        if (other.nativeCase != null || overwite) {
            nativeCase = other.nativeCase;
        }

        computeMultiTenantId = other.computeMultiTenantId;
    }

    public SQLDirectoryDescriptor clone() {
        SQLDirectoryDescriptor clone = new SQLDirectoryDescriptor();
        clone.name = name;
        clone.schemaName = schemaName;
        clone.parentDirectory = parentDirectory;
        clone.dataSourceName = dataSourceName;
        clone.dbDriver = dbDriver;
        clone.dbUrl = dbUrl;
        clone.dbUser = dbUser;
        clone.dbPassword = dbPassword;
        clone.tableName = tableName;
        if (initDependencies != null) {
            clone.initDependencies = new ArrayList<String>(initDependencies);
        }
        clone.idField = idField;
        clone.dataFileName = dataFileName;
        clone.dataFileCharacterSeparator = dataFileCharacterSeparator;
        clone.createTablePolicy = createTablePolicy;
        clone.substringMatchType = substringMatchType;
        clone.autoincrementIdField = autoincrementIdField;
        clone.readOnly = readOnly;
        clone.passwordField = passwordField;
        clone.passwordHashAlgorithm = passwordHashAlgorithm;
        clone.querySizeLimit = querySizeLimit;
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
        clone.remove = remove;
        clone.cacheTimeout = cacheTimeout;
        clone.cacheMaxSize = cacheMaxSize;
        if (staticFilters != null) {
            clone.staticFilters = new SQLStaticFilter[staticFilters.length];
            for (int i = 0; i < staticFilters.length; i++) {
                clone.staticFilters[i] = staticFilters[i].clone();
            }
        }
        clone.nativeCase = nativeCase;
        clone.computeMultiTenantId = computeMultiTenantId;
        return clone;
    }
}
