/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * This {@link ObjectResolver} allows to manage integrity for fields containing references to directory's entry.
 * <p>
 * References contains the directory entry id.
 * </p>
 * <p>
 * To use it, put the following code in your schema XSD (don't forget the directory name):
 * </p>
 *
 * <pre>
 * {@code
 * <xs:element name="carBrand">
 *   <xs:simpleType>
 *     <xs:restriction base="xs:string" ref:resolver="directoryResolver" ref:directory="carBrandsDirectory" />
 *   </xs:simpleType>
 * </xs:element>
 * </pre>
 * <p>
 * For hierarchical directories, which entries reference other entries. You can manage a specific reference containing
 * the full entry path. You have to specify the parent field and the separator used to encode the reference.
 * </p>
 *
 * <pre>
 * {@code
 * <xs:element name="coverage">
 *   <xs:simpleType>
 *     <xs:restriction base="xs:string" ref:resolver="directoryResolver" ref:directory="l10ncoverage" ref:parentField="parent" ref:separator="/" />
 *   </xs:simpleType>
 * </xs:element>
 * </pre>
 * <p>
 * It's not necessary to define parentField and separator for directory using schema ending by xvocabulary. The feature
 * is automatically enable.
 * </p>
 *
 * @since 7.1
 */
public class DirectoryEntryResolver implements ObjectResolver {

    public static final String NAME = "directoryResolver";

    public static final String PARAM_DIRECTORY = "directory";

    private String idField;

    private String schema;

    private Directory directory;

    private Map<String, Serializable> parameters;

    private DirectoryService directoryService;

    private boolean hierarchical = false;

    private String parentField = null;

    private String separator = null;

    public DirectoryService getDirectoryService() {
        if (directoryService == null) {
            directoryService = Framework.getService(DirectoryService.class);
        }
        return directoryService;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    private List<Class<?>> managedClasses = null;

    @Override
    public List<Class<?>> getManagedClasses() {
        if (managedClasses == null) {
            managedClasses = new ArrayList<Class<?>>();
            managedClasses.add(DirectoryEntry.class);
        }
        return managedClasses;
    }

    @Override
    public void configure(Map<String, String> parameters) throws IllegalArgumentException, IllegalStateException {
        if (this.parameters != null) {
            throw new IllegalStateException("cannot change configuration, may be already in use somewhere");
        }
        String directoryName = parameters.get(PARAM_DIRECTORY);
        if (directoryName != null) {
            directoryName = directoryName.trim();
        }
        if (directoryName == null || directoryName.isEmpty()) {
            throw new IllegalArgumentException("missing directory parameter. A directory name is necessary");
        }
        directory = getDirectoryService().getDirectory(directoryName);
        if (directory == null) {
            throw new IllegalArgumentException(String.format("the directory \"%s\" was not found", directoryName));
        }
        idField = directory.getIdField();
        schema = directory.getSchema();
        if (schema.endsWith("xvocabulary")) {
            hierarchical = true;
            parentField = "parent";
            separator = "/";
        }
        String parentFieldParam = StringUtils.trim(parameters.get("parentField"));
        String separatorParam = StringUtils.trim(parameters.get("separator"));
        if (!StringUtils.isBlank(parentFieldParam) && !StringUtils.isBlank(separatorParam)) {
            hierarchical = true;
            parentField = parentFieldParam;
            separator = separatorParam;
        }
        this.parameters = new HashMap<String, Serializable>();
        this.parameters.put(PARAM_DIRECTORY, directory.getName());
    }

    @Override
    public String getName() {
        checkConfig();
        return NAME;
    }

    @Override
    public Map<String, Serializable> getParameters() {
        checkConfig();
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean validate(Object value) throws IllegalStateException {
        checkConfig();
        return fetch(value) != null;
    }

    @Override
    public Object fetch(Object value) throws IllegalStateException {
        checkConfig();
        if (value != null && value instanceof String) {
            String id = (String) value;
            if (hierarchical) {
                String[] ids = StringUtils.split(id, separator);
                if (ids.length > 0) {
                    id = ids[ids.length - 1];
                } else {
                    return null;
                }
            }
            Session session = directory.getSession();
            try {
                DocumentModel doc = session.getEntry(id);
                if (doc != null) {
                    return new DirectoryEntry(directory.getName(), doc);
                }
                return null;
            } finally {
                session.close();
            }
        }
        return null;
    }

    @Override
    public <T> T fetch(Class<T> type, Object value) throws IllegalStateException {
        checkConfig();
        DirectoryEntry doc = (DirectoryEntry) fetch(value);
        if (doc != null) {
            if (type.isInstance(doc)) {
                return type.cast(doc);
            }
            if (type.isInstance(doc.getDocumentModel())) {
                return type.cast(doc.getDocumentModel());
            }
        }
        return null;
    }

    @Override
    public Serializable getReference(Object entity) throws IllegalStateException {
        checkConfig();
        DocumentModel entry = null;
        if (entity != null) {
            if (entity instanceof DirectoryEntry) {
                entry = ((DirectoryEntry) entity).getDocumentModel();
            } else if (entity instanceof DocumentModel) {
                entry = (DocumentModel) entity;
            }
            if (entry != null) {
                if (!entry.hasSchema(schema)) {
                    return null;
                }
                String result = (String) entry.getProperty(schema, idField);
                if (hierarchical) {
                    String parent = (String) entry.getProperty(schema, parentField);
                    DocumentModel parentModel;
                    Session session = null;
                    try {
                        while (parent != null) {
                            if (session == null) {
                                session = directory.getSession();
                            }
                            parentModel = session.getEntry(parent);
                            if (parentModel != null) {
                                parent = (String) entry.getProperty(schema, idField);
                                result = parent + separator + result;
                                parent = (String) entry.getProperty(schema, parentField);
                            }
                        }
                    } finally {
                        if (session != null) {
                            session.close();
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        checkConfig();
        return Helper.getConstraintErrorMessage(this, invalidValue, locale, directory.getName());
    }

    private void checkConfig() throws IllegalStateException {
        if (parameters == null) {
            throw new IllegalStateException(
                    "you should call #configure(Map<String, String>) before. Please get this resolver throught ExternalReferenceService which is in charge of resolver configuration.");
        }
    }

}
