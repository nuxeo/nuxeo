/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
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

    private static final long serialVersionUID = 1L;

    public static final String NAME = "directoryResolver";

    public static final String PARAM_DIRECTORY = "directory";

    public static final String PARAM_PARENT_FIELD = "parentField";

    public static final String PARAM_SEPARATOR = "separator";

    private String idField;

    private String schema;

    private Map<String, Serializable> parameters;

    private boolean hierarchical = false;

    private String parentField = null;

    private String separator = null;

    private List<Class<?>> managedClasses = null;

    private String directoryName;

    @Override
    public void configure(Map<String, String> parameters) throws IllegalArgumentException, IllegalStateException {
        if (this.parameters != null) {
            throw new IllegalStateException("cannot change configuration, may be already in use somewhere");
        }
        directoryName = parameters.get(PARAM_DIRECTORY);
        if (directoryName != null) {
            directoryName = directoryName.trim();
        }
        if (directoryName == null || directoryName.isEmpty()) {
            throw new IllegalArgumentException("missing directory parameter. A directory name is necessary");
        }
        Directory directory = getDirectory();
        idField = directory.getIdField();
        schema = directory.getSchema();
        if (schema.endsWith("xvocabulary")) {
            hierarchical = true;
            parentField = "parent";
            separator = "/";
        }
        String parentFieldParam = StringUtils.trim(parameters.get(PARAM_PARENT_FIELD));
        String separatorParam = StringUtils.trim(parameters.get(PARAM_SEPARATOR));
        if (!StringUtils.isBlank(parentFieldParam) && !StringUtils.isBlank(separatorParam)) {
            hierarchical = true;
            parentField = parentFieldParam;
            separator = separatorParam;
        }
        this.parameters = new HashMap<>();
        this.parameters.put(PARAM_DIRECTORY, directoryName);
    }

    @Override
    public List<Class<?>> getManagedClasses() {
        if (managedClasses == null) {
            managedClasses = new ArrayList<>();
            managedClasses.add(DirectoryEntry.class);
        }
        return managedClasses;
    }

    public Directory getDirectory() {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Directory directory = directoryService.getDirectory(directoryName);
        if (directory == null) {
            throw new IllegalArgumentException(String.format("the directory \"%s\" was not found", directoryName));
        }
        return directory;
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
        if (value instanceof String) {
            String id = (String) value;
            if (hierarchical) {
                String[] ids = StringUtils.split(id, separator);
                if (ids.length > 0) {
                    id = ids[ids.length - 1];
                } else {
                    return null;
                }
            }
            try (Session session = getDirectory().getSession()) {
                String finalId = id; // Effectively final
                DocumentModel doc = Framework.doPrivileged(() -> session.getEntry(finalId));
                if (doc != null) {
                    return new DirectoryEntry(directoryName, doc);
                }
                return null;
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
                    try (Session session = getDirectory().getSession()) {
                        while (parent != null) {
                            String finalParent = parent; // Effectively final
                            entry = Framework.doPrivileged(() -> session.getEntry(finalParent));
                            if (entry == null) {
                                break;
                            }
                            result = parent + separator + result;
                            parent = (String) entry.getProperty(schema, parentField);
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
        return Helper.getConstraintErrorMessage(this, invalidValue, locale, directoryName);
    }

    private void checkConfig() throws IllegalStateException {
        if (parameters == null) {
            throw new IllegalStateException(
                    "you should call #configure(Map<String, String>) before. Please get this resolver throught ExternalReferenceService which is in charge of resolver configuration.");
        }
    }

}
