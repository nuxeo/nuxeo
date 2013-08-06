/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */

package org.nuxeo.ecm.platform.ui.select2;

import java.io.BufferedOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.writers.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@Name("select2Actions")
@Scope(ScopeType.EVENT)
public class Select2ActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(Select2ActionsBean.class);

    protected static final String SELECT2_RESOURCES_MARKER = "SELECT2_RESOURCES_MARKER";

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected transient CoreSession dedicatedSession = null;

    @Destroy
    public void destroy() {
        if (dedicatedSession != null) {
            CoreInstance.getInstance().close(dedicatedSession);
        }
    }

    /**
     * Encode widget properties and parameters that Select2 pick them up in a
     * hidden input.
     *
     * @param widget the widget
     * @return encoded
     * @throws Exception
     *
     * @since 5.7.3
     */
    public String encodeParameters(final Widget widget) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);

        JsonGenerator jg = JsonWriter.createGenerator(out);
        jg.writeStartObject();

        // multiple is not in properties and we must add it because Select2
        // needs to know.
        jg.writeStringField("multiple", "" + isMultiSelection(widget));

        final boolean isTranslated = widget.isTranslated();

        // The operation might need to know if we translate the labels or not.
        jg.writeStringField("translateLabels", "" + isTranslated);

        Map<String, Serializable> propertySet = widget.getProperties();
        boolean hasPlaceholder = false;

        for (Entry<String, Serializable> entry : propertySet.entrySet()) {

            String value = entry.getValue().toString();

            if (entry.getKey().equals(Select2Common.PLACEHOLDER)) {
                hasPlaceholder = true;
                // placeholder can be translated
                if (isTranslated) {
                    value = messages.get(entry.getValue().toString());
                }
            }

            jg.writeStringField(entry.getKey(), value);

        }

        if (!hasPlaceholder) {
            // No placeholder provider and Select2 requires one to enable the
            // reset button.
            jg.writeStringField(Select2Common.PLACEHOLDER,
                    messages.get("label.vocabulary.selectValue"));
        }

        jg.writeEndObject();
        jg.flush();
        out.flush();
        return new String(baos.toByteArray(), "UTF-8");
    }

    /*public String getKeySeparator(final Widget widget) {
        String keySeparator = (String) widget.getProperties().get(
                "keySeparator");
        if (keySeparator == null || keySeparator.isEmpty()) {
            return Select2Common.DEFAULT_KEY_SEPARATOR;
        } else {
            return keySeparator;
        }
    }*/

    protected LayoutStore getLayoutStore() {
        LayoutStore layoutStore = null;
        try {
            layoutStore = Framework.getLocalService(LayoutStore.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (layoutStore == null) {
            throw new RuntimeException("Missing service for LayoutStore");
        }
        return layoutStore;
    }

    protected CoreSession getRepositorySession(final String repoName)
            throws ClientException {

        RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
        Repository repository = null;
        if (repoName == null || repoName.isEmpty()) {
            repository = rm.getDefaultRepository();
        } else {
            repository = rm.getRepository(repoName);
            if (repository == null) {
                log.error("Unable to resolve repository " + repoName);
                return null;
            }
        }

        if (documentManager != null
                && documentManager.getRepositoryName().equals(
                        repository.getName())) {
            return documentManager;
        }

        try {
            dedicatedSession = repository.open();
            return dedicatedSession;
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    public boolean isMultiSelection(final Widget widget) {
        WidgetTypeDefinition widgetTypeDefinition = getLayoutStore().getWidgetTypeDefinition(
                "jsf", widget.getType());
        return widgetTypeDefinition.getConfiguration().isList();
    }

    public boolean mustIncludeResources() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

            if (request.getAttribute(SELECT2_RESOURCES_MARKER) != null) {
                return false;
            } else {
                request.setAttribute(SELECT2_RESOURCES_MARKER, "done");
                return true;
            }
        }
        return false;
    }

    protected JSONObject resolveDirectoryEntry(final String storedReference,
            String keySeparator, final Session session,
            final Schema schema, final String label,
            final boolean translateLabels, final boolean dbl10n) {
        if (storedReference == null || storedReference.isEmpty()) {
            log.trace("No reference provided ");
            return null;
        }

        if (keySeparator == null || keySeparator.isEmpty()) {
            keySeparator = Select2Common.DEFAULT_KEY_SEPARATOR;
        }

        String entryId = storedReference.substring(
                storedReference.lastIndexOf(keySeparator) + 1,
                storedReference.length());

        try {
            DocumentModel result = session.getEntry(entryId);
            if (result == null) {
                log.warn("Unable to resolve entry " + storedReference);
                return null;
            }

            JSONObject obj = new JSONObject();
            for (Field field : schema.getFields()) {
                QName fieldName = field.getName();
                String key = fieldName.getLocalName();
                Serializable value = result.getPropertyValue(fieldName.getPrefixedName());
                if (label.equals(key)) {
                    if (translateLabels && !dbl10n) {
                        value = messages.get(value);
                    }
                    obj.element(Select2Common.LABEL, value);
                } else {
                    obj.element(key, value);
                }
            }
            obj.element(Select2Common.COMPUTED_ID, storedReference);
            return obj;
        } catch (ClientException e) {
            log.error("An error occured while resolving directoryEntry", e);
            return null;
        }
    }

    public String resolveMultipleDirectoryEntries(final Object value,
            final String directoryName, final boolean translateLabels,
            String keySeparator, final boolean dbl10n,
            final String labelFieldName) {
        JSONArray result = getMultipleDirectoryEntries(value, directoryName, translateLabels, keySeparator, dbl10n, labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "[]";
        }
    }

    public JSONArray getMultipleDirectoryEntries(final Object value,
            final String directoryName, final boolean translateLabels,
            String keySeparator, final boolean dbl10n,
            final String labelFieldName) {
        JSONArray result = new JSONArray();
        if (value == null) {
            return result;
        }

        List<String> storedRefs = new ArrayList<>();
        if (value instanceof List) {
            for (Object v : (List) value) {
                storedRefs.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                storedRefs.add(v.toString());
            }
        }

        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        Directory directory = null;
        Session session = null;
        try {
            directory = directoryService.getDirectory(directoryName);
            if (directory == null) {
                log.error("Could not find directory with name " + directoryName);
                return result;
            }
            session = directory.getSession();
            String schemaName = directory.getSchema();
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            Schema schema = schemaManager.getSchema(schemaName);
            final Locale locale = org.jboss.seam.core.Locale.instance();
            final String label = Select2Common.getLabelFieldName(schema,
                    dbl10n, labelFieldName, locale.getLanguage());

            for (String ref : storedRefs) {
                JSONObject obj = resolveDirectoryEntry(ref, keySeparator,
                        session, schema, label, translateLabels, dbl10n);
                if (obj != null)
                    result.add(obj);
            }
            return result;
        } catch (DirectoryException de) {
            log.error("An error occured while obtaining directory "
                    + directoryName, de);
            return result;
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (ClientException ce) {
                log.error("Could not close directory session", ce);
            }
        }

    }

    @SuppressWarnings("rawtypes")
    public List<String> resolveMultipleDirectoryEntryLabels(final Object value,
            final String directoryName, final boolean translateLabels,
            final String keySeparator, final boolean dbl10n,
            final String labelFieldName) {
        List<String> result = new ArrayList<String>();
        JSONArray array = getMultipleDirectoryEntries(value, directoryName, translateLabels, keySeparator, dbl10n, labelFieldName);
        if (array != null) {
            for (int i = 0; i < array.size();  i++) {
                result.add(array.getJSONObject(i).getString(Select2Common.LABEL));
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public List<String> resolveMultipleReferenceLabels(final Object value,
            final String repo, final String operationName,
            final String idProperty, final String label) throws Exception {

        List<String> result = new ArrayList<>();

        if (value == null) {
            return result;
        }

        List<String> storedRefs = new ArrayList<>();
        if (value instanceof List) {
            for (Object v : (List) value) {
                storedRefs.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                storedRefs.add(v.toString());
            }
        }

        for (String ref : storedRefs) {
            DocumentModel doc = resolveReference(repo, ref, operationName,
                    idProperty);
            if (doc != null) {
                if (label != null && !label.isEmpty()) {
                    Object val = doc.getPropertyValue(label);
                    if (val == null) {
                        result.add("");
                    } else {
                        result.add(val.toString());
                    }
                } else {
                    result.add(doc.getTitle());
                }
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public String resolveMultipleReferences(final Object value,
            final String repo, final String operationName,
            final String idProperty, final String schemaNames) throws Exception {

        if (value == null) {
            return "[]";
        }

        List<String> storedRefs = new ArrayList<>();
        if (value instanceof List) {
            for (Object v : (List) value) {
                storedRefs.add(v.toString());
            }
        } else if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                storedRefs.add(v.toString());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonGenerator jg = JsonWriter.createGenerator(out);
        String[] schemas = null;
        if (schemaNames != null && !schemaNames.isEmpty()) {
            schemas = schemaNames.split(",");
        }
        jg.writeStartArray();

        for (String ref : storedRefs) {
            DocumentModel doc = resolveReference(repo, ref, operationName,
                    idProperty);
            if (doc == null) {
                return "";
            }
            JsonDocumentWriter.writeDocument(jg, doc, schemas);
        }

        jg.writeEndArray();
        out.flush();
        String json = new String(baos.toByteArray(), "UTF-8");

        if (!json.endsWith("]")) {
            // XXX !!!
            json = json + "]";
        }

        return json;
    }

    protected DocumentModel resolveReference(final String repo,
            final String storedReference, final String operationName,
            final String idProperty) throws Exception {

        if (storedReference == null || storedReference.isEmpty()) {
            log.trace("No reference provided ");
            return null;
        }
        DocumentModel doc = null;
        CoreSession session;
        try {
            session = getRepositorySession(repo);
            if (session == null) {
                log.error("Unable to get CoreSession for repo " + repo);
                return null;
            }
            if (operationName == null || operationName.isEmpty()) {
                DocumentRef ref = null;

                if (idProperty != null && !idProperty.isEmpty()) {
                    String query = " select * from Document where "
                            + idProperty + "='" + storedReference + "'";
                    DocumentModelList docs = session.query(query);
                    if (docs.size() > 0) {
                        return docs.get(0);
                    } else {
                        log.warn("Unable to resolve doc using property "
                                + idProperty + " and value " + storedReference);
                        return null;
                    }
                } else {
                    if (storedReference.startsWith("/")) {
                        ref = new PathRef(storedReference);
                    } else {
                        ref = new IdRef(storedReference);
                    }
                    if (session.exists(ref)) {
                        doc = session.getDocument(ref);
                    } else {
                        log.warn("Unable to resolve reference on " + ref);
                    }
                }
            } else {
                AutomationService as = Framework.getLocalService(AutomationService.class);
                OperationContext ctx = new OperationContext(session);

                ctx.put("value", storedReference);
                ctx.put("xpath", idProperty);

                Object result = as.run(ctx, operationName, null);

                if (result == null) {
                    log.warn("Unable to resolve reference " + storedReference
                            + " using property " + idProperty
                            + " and operation" + operationName);
                    doc = null;
                } else if (result instanceof DocumentModel) {
                    doc = (DocumentModel) result;
                } else if (result instanceof DocumentModelList) {
                    DocumentModelList docs = (DocumentModelList) result;
                    if (docs.size() > 0) {
                        doc = docs.get(0);
                    } else {
                        log.warn("No document found");
                    }
                }
            }
            return doc;
        } catch (ClientException e) {
            log.error("Unable to resolve reference", e);
        } catch (InvalidChainException e) {
            log.error("Unable to resolve reference", e);
        } catch (OperationException e) {
            log.error("Unable to resolve reference", e);
        }
        return doc;

    }


    public String resolveSingleDirectoryEntry(final String storedReference,
            final String directoryName, final boolean translateLabels,
            String keySeparator, final boolean dbl10n,
            final String labelFieldName) {
        JSONObject result = getSingleDirectoryEntry(storedReference, directoryName, translateLabels, keySeparator, dbl10n, labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    public JSONObject getSingleDirectoryEntry(final String storedReference,
            final String directoryName, final boolean translateLabels,
            String keySeparator, final boolean dbl10n,
            final String labelFieldName) {

        if (storedReference == null || storedReference.isEmpty()) {
            return null;
        }

        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        Directory directory = null;
        Session session = null;
        try {
            directory = directoryService.getDirectory(directoryName);
            if (directory == null) {
                log.error("Could not find directory with name " + directoryName);
                return null;
            }
            session = directory.getSession();
            String schemaName = directory.getSchema();
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            Schema schema = schemaManager.getSchema(schemaName);

            final Locale locale = org.jboss.seam.core.Locale.instance();
            final String label = Select2Common.getLabelFieldName(schema,
                    dbl10n, labelFieldName, locale.getLanguage());

            JSONObject obj = resolveDirectoryEntry(storedReference,
                    keySeparator, session, schema, label, translateLabels,
                    dbl10n);

            return obj;
        } catch (DirectoryException de) {
            log.error("An error occured while obtaining directory "
                    + directoryName, de);
            return null;
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (ClientException ce) {
                log.error("Could not close directory session", ce);
            }
        }

    }

    public String resolveSingleDirectoryEntryLabel(
            final String storedReference, final String directoryName,
            final boolean translateLabels, String keySeparator,
            final boolean dbl10n, final String labelFieldName) {
        JSONObject obj = getSingleDirectoryEntry(storedReference, directoryName, translateLabels, keySeparator, dbl10n, labelFieldName);
        if (obj == null) {
            return "";
        }
        return obj.getString(Select2Common.LABEL);
    }

    public String resolveSingleReference(final String storedReference,
            final String repo, final String operationName,
            final String idProperty, final String schemaNames) throws Exception {

        DocumentModel doc;
        doc = resolveReference(repo, storedReference, operationName, idProperty);
        if (doc == null) {
            return "";
        }
        String[] schemas = null;
        if (schemaNames != null && !schemaNames.isEmpty()) {
            schemas = schemaNames.split(",");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonDocumentWriter.writeDocument(out, doc, schemas);
        out.flush();
        return new String(baos.toByteArray(), "UTF-8");

    }

    public String resolveSingleReferenceLabel(final String storedReference,
            final String repo, final String operationName,
            final String idProperty, final String label) throws Exception {

        DocumentModel doc = resolveReference(repo, storedReference,
                operationName, idProperty);
        if (doc == null) {
            return "";
        }

        if (label != null && !label.isEmpty()) {
            Object val = doc.getPropertyValue(label);
            if (val == null) {
                return "";
            } else {
                return val.toString();
            }
        }
        return doc.getTitle();
    }

}
