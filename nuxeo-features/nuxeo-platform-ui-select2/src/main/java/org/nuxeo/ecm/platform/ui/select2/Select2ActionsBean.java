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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
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
import org.jboss.seam.contexts.FacesLifecycle;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
import org.nuxeo.ecm.platform.ui.select2.automation.SuggestUserEntries;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.codec.DocumentIdCodec;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Initialization for Select2.
 *
 * @since 5.7.3
 */
@Name("select2Actions")
@Scope(ScopeType.EVENT)
public class Select2ActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(Select2ActionsBean.class);

    protected static final String SELECT2_RESOURCES_MARKER = "SELECT2_RESOURCES_MARKER";

    private static List<String> formatList(JSONArray array) {
        List<String> result = new ArrayList<String>();
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                result.add(array.getJSONObject(i).getString(Select2Common.LABEL));
            }
        }
        return result;
    }

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

        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        jg.writeStartObject();

        // multiple is not in properties and we must add it because Select2
        // needs to know.
        jg.writeStringField("multiple", "" + isMultiSelection(widget));

        final boolean isTranslated = widget.isTranslated();

        // The operation might need to know if we translate the labels or not.
        jg.writeStringField("translateLabels", "" + isTranslated);

        Map<String, Serializable> propertySet = widget.getProperties();
        boolean hasPlaceholder = false;
        boolean hasSuggestionFormatter = false;
        boolean hasSelectionFormatter = false;
        boolean hasAjaxReRender = false;
        boolean hasWidth = false;
        boolean hasMinChars = false;

        for (Entry<String, Serializable> entry : propertySet.entrySet()) {

            String value = entry.getValue().toString();

            if (entry.getKey().equals(Select2Common.PLACEHOLDER)) {
                hasPlaceholder = true;
                // placeholder can be translated
                if (isTranslated) {
                    value = messages.get(entry.getValue().toString());
                }
            } else if (entry.getKey().equals(Select2Common.SUGGESTION_FORMATTER)) {
                hasSuggestionFormatter = true;
            } else if (entry.getKey().equals(Select2Common.SELECTION_FORMATTER)) {
                hasSelectionFormatter = true;
            } else if (entry.getKey().equals(Select2Common.AJAX_RERENDER)) {
                hasAjaxReRender = true;
            } else if (entry.getKey().equals(Select2Common.WIDTH)) {
                hasWidth = true;
            } else if (entry.getKey().equals(Select2Common.MIN_CHARS)) {
                hasMinChars = true;
            }

            jg.writeStringField(entry.getKey(), value);

        }

        if (!hasPlaceholder) {
            // No placeholder provider and Select2 requires one to enable the
            // reset button.
            jg.writeStringField(Select2Common.PLACEHOLDER,
                    messages.get("label.vocabulary.selectValue"));
        }

        // Specifc stuff for widget type
        if (Select2Common.SELECT2_USER_WIDGET_TYPE_LIST.contains(widget.getType())) {
            jg.writeStringField("operationId", SuggestUserEntries.ID);
            // add default selection and suggestion formatter if needed.
            if (!hasSuggestionFormatter) {
                jg.writeStringField(Select2Common.SUGGESTION_FORMATTER,
                        Select2Common.USER_DEFAULT_SUGGESTION_FORMATTER);
            }
            if (!hasSelectionFormatter) {
                jg.writeStringField(Select2Common.SELECTION_FORMATTER,
                        Select2Common.USER_DEFAULT_SELECTION_FORMATTER);
            }
        } else if (Select2Common.SELECT2_DOC_WIDGET_TYPE_LIST.contains(widget.getType())) {
            if (!hasSuggestionFormatter) {
                jg.writeStringField(Select2Common.SUGGESTION_FORMATTER,
                        Select2Common.DOC_DEFAULT_SUGGESTION_FORMATTER);
            }
            if (!hasSelectionFormatter) {
                jg.writeStringField(Select2Common.SELECTION_FORMATTER,
                        Select2Common.DOC_DEFAULT_SELECTION_FORMATTER);
            }
        } else if (Select2Common.SELECT2_DIR_WIDGET_TYPE_LIST.contains(widget.getType())) {
            if (!hasSuggestionFormatter) {
                jg.writeStringField(Select2Common.SUGGESTION_FORMATTER,
                        Select2Common.DIR_DEFAULT_SUGGESTION_FORMATTER);
            }
            if (!hasSelectionFormatter) {
                jg.writeStringField(Select2Common.SELECTION_FORMATTER,
                        Select2Common.DIR_DEFAULT_SELECTION_FORMATTER);
            }
        }

        //
        if (!hasWidth) {
            jg.writeStringField(Select2Common.WIDTH,
                    Select2Common.DEFAULT_WIDTH);
        }

        if (!hasMinChars) {
            jg.writeNumberField(Select2Common.MIN_CHARS, Select2Common.DEFAULT_MIN_CHARS);
        }

        // Are we writing or reading
        boolean readonly = !widget.getMode().equals("edit")
                && !widget.getMode().equals("create");
        jg.writeStringField(Select2Common.READ_ONLY_PARAM,
                Boolean.toString(readonly));
        if (hasAjaxReRender) {
            jg.writeStringField(Select2Common.RERENDER_JS_FUNCTION_NAME,
                    widget.getId() + "_reRender");
        }

        jg.writeEndObject();
        jg.flush();
        out.flush();
        return new String(baos.toByteArray(), "UTF-8");
    }

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

    protected JSONArray getMultipleDirectoryEntries(final Object value,
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
                if (obj != null) {
                    result.add(obj);
                }
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

    protected JSONArray getMultipleUserReference(final Object value,
            final boolean prefixed) {
        if (value == null) {
            return null;
        }
        JSONArray result = new JSONArray();
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
            JSONObject resolved = getSingleUserReference(ref, prefixed);
            if (resolved != null && !resolved.isEmpty()) {
                result.add(resolved);
            }
        }
        return result;
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

    protected JSONObject getSingleDirectoryEntry(final String storedReference,
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

    protected JSONObject getSingleUserReference(final String storedReference,
            final boolean prefixed) {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        JSONObject obj = new JSONObject();
        if (storedReference == null || storedReference.isEmpty()) {
            return null;
        }
        try {
            DocumentModel user = null;
            DocumentModel group = null;
            if (prefixed) {
                if (storedReference.startsWith(NuxeoPrincipal.PREFIX)) {
                    user = userManager.getUserModel(storedReference.substring(NuxeoPrincipal.PREFIX.length()));
                } else if (storedReference.startsWith(NuxeoGroup.PREFIX)) {
                    group = userManager.getGroupModel(storedReference.substring(NuxeoGroup.PREFIX.length()));
                } else {
                    log.warn("User reference is prefixed but prefix was not found on reference: "
                            + storedReference);
                    return null;
                }
            } else {
                user = userManager.getUserModel(storedReference);
                if (user == null) {
                    group = userManager.getGroupModel(storedReference);
                }
            }
            if (user != null) {
                Schema schema = schemaManager.getSchema("user");
                String username = null;
                String firstname = null;
                String lastname = null;
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = user.getPropertyValue(fieldName.getPrefixedName());
                    if (key.equals("password")) {
                        continue;
                    }
                    obj.element(key, value);
                    if (key.equals("username")) {
                        username = (String) value;
                    } else if (key.equals("firstName")) {
                        firstname = (String) value;
                    } else if (key.equals("lastName")) {
                        lastname = (String) value;
                    }
                }
                String label = "";
                if (firstname != null && !firstname.isEmpty()
                        && lastname != null && !lastname.isEmpty()) {
                    label = firstname + " " + lastname;
                } else {
                    label = username;
                }
                String userId = user.getId();
                obj.put(Select2Common.ID, userId);
                obj.put(Select2Common.LABEL, label);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.USER_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME,
                        NuxeoPrincipal.PREFIX + userId);
            } else if (group != null) {
                Schema schema = schemaManager.getSchema("group");
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = group.getPropertyValue(fieldName.getPrefixedName());
                    obj.element(key, value);
                    if (key.equals("grouplabel")) {
                        obj.element(Select2Common.LABEL, value);
                    }
                }
                String groupId = group.getId();
                obj.put(Select2Common.ID, groupId);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.GROUP_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX
                        + groupId);
            } else {
                log.warn("Could not resolve user or group reference: "
                        + storedReference);
                return null;
            }
        } catch (ClientException e) {
            log.error("An error occured while retrieving user or group reference: "
                    + storedReference);
            return null;
        }
        return obj;
    }

    public boolean isMultiSelection(final Widget widget) {
        WidgetTypeDefinition widgetTypeDefinition = getLayoutStore().getWidgetTypeDefinition(
                "jsf", widget.getType());
        return widgetTypeDefinition.getConfiguration().isList();
    }

    public boolean mustIncludeResources() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.getRenderResponse()) {
            PhaseId currentPhaseId = FacesLifecycle.getPhaseId();
            if (currentPhaseId.equals(PhaseId.RENDER_RESPONSE)) {
                HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();

                if (request.getAttribute(SELECT2_RESOURCES_MARKER) != null) {
                    return false;
                } else {
                    request.setAttribute(SELECT2_RESOURCES_MARKER, "done");
                    return true;
                }
            }
        }
        return false;
    }

    protected JSONObject createNotFoundEntry(final String id) {
        return createEntryWithWarnMessage(id, "entry not found");
    }

    protected JSONObject createEntryWithWarnMessage(final String label,
            final String warnMessage) {
        JSONObject obj = new JSONObject();
        obj.put(Select2Common.LABEL, label);
        obj.put(Select2Common.WARN_MESSAGE_LABEL, warnMessage);
        return obj;
    }

    protected JSONObject resolveDirectoryEntry(final String storedReference,
            String keySeparator, final Session session, final Schema schema,
            final String label, final boolean translateLabels,
            final boolean dbl10n) {
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
                return createNotFoundEntry(storedReference);
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

            // Add a warning message if the entity is obsolete
            if (obj.containsKey(Select2Common.OBSOLETE_FIELD_ID)
                    && obj.getInt(Select2Common.OBSOLETE_FIELD_ID) > 0) {
                obj.element(Select2Common.WARN_MESSAGE_LABEL,
                        messages.get("obsolete"));
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
        JSONArray result = getMultipleDirectoryEntries(value, directoryName,
                translateLabels, keySeparator, dbl10n, labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "[]";
        }
    }

    public List<String> resolveMultipleDirectoryEntryLabels(final Object value,
            final String directoryName, final boolean translateLabels,
            final String keySeparator, final boolean dbl10n,
            final String labelFieldName) {
        return formatList(getMultipleDirectoryEntries(value, directoryName,
                translateLabels, keySeparator, dbl10n, labelFieldName));
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
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
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

    @SuppressWarnings("rawtypes")
    public String resolveMultipleUserReference(final Object value,
            final boolean prefixed) {
        if (value == null) {
            return "[]";
        }
        JSONArray result = new JSONArray();
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
            String resolved = resolveSingleUserReference(ref, prefixed);
            if (resolved != null && !resolved.isEmpty()) {
                result.add(resolved);
            }
        }
        return result.toString();
    }

    public List<String> resolveMultipleUserReferenceLabels(final Object value,
            final boolean prefixed) {
        return formatList(getMultipleUserReference(value, prefixed));
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
        JSONObject result = getSingleDirectoryEntry(storedReference,
                directoryName, translateLabels, keySeparator, dbl10n,
                labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    public String resolveSingleDirectoryEntryLabel(
            final String storedReference, final String directoryName,
            final boolean translateLabels, String keySeparator,
            final boolean dbl10n, final String labelFieldName) {
        JSONObject obj = getSingleDirectoryEntry(storedReference,
                directoryName, translateLabels, keySeparator, dbl10n,
                labelFieldName);
        if (obj == null) {
            return "";
        }
        return obj.optString(Select2Common.LABEL);
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

        DocumentIdCodec documentIdCodec = new DocumentIdCodec();
        Map<String, String> contextParameters = new HashMap<String, String>();
        contextParameters.put("documentURL", documentIdCodec.getUrlFromDocumentView(new DocumentViewImpl(doc)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, doc, schemas, contextParameters);
        jg.flush();
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

    public String resolveSingleUserReference(final String storedReference,
            final boolean prefixed) {
        JSONObject result = getSingleUserReference(storedReference, prefixed);
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    public String resolveUserReferenceLabel(final String storedReference,
            final boolean prefixed) {
        JSONObject obj = getSingleUserReference(storedReference, prefixed);
        if (obj == null) {
            return "";
        }
        return obj.optString(Select2Common.LABEL);
    }

}
