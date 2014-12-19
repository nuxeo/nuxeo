package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class DirectoryEntryResolver implements ObjectResolver {

    public static final String NAME = "directoryResolver";

    public static final String PARAM_DIRECTORY = "directory";

    private String idField;

    private String schema;

    private Directory directory;

    private Map<String, Serializable> parameters;

    private DirectoryService directoryService;

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
        if (value != null && value instanceof String) {
            String id = (String) value;
            Session session = null;
            try {
                session = directory.getSession();
                return session.hasEntry(id);
            } finally {
                if (session != null) {
                    try {
                        session.close();
                        session = null;
                    } catch (Exception e) {
                        session = null;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Object fetch(Object value) throws IllegalStateException {
        checkConfig();
        if (value != null && value instanceof String) {
            String id = (String) value;
            Session session = null;
            try {
                session = directory.getSession();
                return session.getEntry(id);
            } finally {
                if (session != null) {
                    try {
                        session.close();
                        session = null;
                    } catch (Exception e) {
                        session = null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public <T> T fetch(Class<T> type, Object value) throws IllegalStateException {
        checkConfig();
        DocumentModel doc = (DocumentModel) fetch(value);
        if (doc != null) {
            if (type.isInstance(doc)) {
                return type.cast(doc);
            }
        }
        return null;
    }

    @Override
    public Serializable getReference(Object entity) throws IllegalStateException {
        checkConfig();
        if (entity != null && entity instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) entity;
            if (!doc.hasSchema(schema)) {
                return null;
            }
            return (Serializable) doc.getProperty(schema, idField);
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
