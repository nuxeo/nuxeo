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
import java.io.IOException;
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
import org.apache.commons.lang.StringUtils;
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
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
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
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.ui.select2.automation.SuggestDirectoryEntries;
import org.nuxeo.ecm.platform.ui.select2.automation.SuggestUserEntries;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.codec.DocumentIdCodec;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.RenderingContextWebUtils;
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
            dedicatedSession.close();
        }
    }

    private static Map<String, String> getDefaultFormattersMap(final String suggestionFormatterName,
            String selectionFormatterName) {
        Map<String, String> result = new HashMap<String, String>();
        result.put(Select2Common.SUGGESTION_FORMATTER, suggestionFormatterName);
        result.put(Select2Common.SELECTION_FORMATTER, selectionFormatterName);
        return result;
    }

    /**
     * @since 5.9.3
     */
    protected static Map<String, String> getContextParameter(final DocumentModel doc) {
        DocumentIdCodec documentIdCodec = new DocumentIdCodec();
        Map<String, String> contextParameters = new HashMap<String, String>();
        contextParameters.put("documentURL", documentIdCodec.getUrlFromDocumentView(new DocumentViewImpl(doc)));
        return contextParameters;
    }

    public String encodeParametersForUserSuggestion(final Widget widget,
            final Map<String, Serializable> resolvedWidgetProperties) {
        Map<String, String> params = getDefaultFormattersMap(Select2Common.USER_DEFAULT_SUGGESTION_FORMATTER,
                Select2Common.USER_DEFAULT_SELECTION_FORMATTER);
        params.put(Select2Common.OPERATION_ID, SuggestUserEntries.ID);
        return encodeParameters(widget, params, resolvedWidgetProperties);
    }

    public String encodeParametersForDirectory(final Widget widget,
            final Map<String, Serializable> resolvedWidgetProperties) {
        Map<String, String> params = getDefaultFormattersMap(Select2Common.DIR_DEFAULT_SUGGESTION_FORMATTER,
                Select2Common.DIR_DEFAULT_SELECTION_FORMATTER);
        params.put(Select2Common.OPERATION_ID, SuggestDirectoryEntries.ID);
        return encodeParameters(widget, params, resolvedWidgetProperties);
    }

    public String encodeParameters(final Widget widget) {
        return encodeParameters(widget, null);
    }

    public String encodeParameters(final Widget widget, final Map<String, Serializable> resolvedWidgetProperties) {
        Map<String, String> params = getDefaultFormattersMap(Select2Common.DOC_DEFAULT_SUGGESTION_FORMATTER,
                Select2Common.DOC_DEFAULT_SELECTION_FORMATTER);
        params.put(Select2Common.OPERATION_ID, DocumentPageProviderOperation.ID);
        return encodeParameters(widget, params, resolvedWidgetProperties);
    }

    /**
     * Encode widget properties and parameters that Select2 pick them up in a hidden input.
     *
     * @param widget the widget
     * @return encoded
     * @since 5.7.3
     */
    public String encodeParameters(final Widget widget, final Map<String, String> defaultParams,
            final Map<String, Serializable> resolvedWidgetProperties) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);

        JsonGenerator jg;
        try {
            jg = JsonHelper.createJsonGenerator(out);

            jg.writeStartObject();

            // multiple is not in properties and we must add it because Select2
            // needs to know.
            jg.writeStringField("multiple", "" + isMultiSelection(widget));

            final boolean isTranslated = widget.isTranslated();

            // Are we writing or reading
            boolean readonly = !widget.getMode().equals("edit") && !widget.getMode().equals("create");
            jg.writeStringField(Select2Common.READ_ONLY_PARAM, Boolean.toString(readonly));

            Map<String, Serializable> propertySet = null;
            if (resolvedWidgetProperties != null) {
                propertySet = resolvedWidgetProperties;
            } else {
                propertySet = widget.getProperties();
            }

            boolean hasPlaceholder = false;
            boolean hasAjaxReRender = false;
            boolean hasWidth = false;
            boolean hasMinChars = false;

            for (Entry<String, Serializable> entry : propertySet.entrySet()) {

                if (entry.getValue() == null) {
                    continue;
                }

                if (defaultParams != null) {
                    // Widget properties have priority on default properties
                    defaultParams.remove(entry.getKey());
                }

                String value = entry.getValue().toString();

                if (entry.getKey().equals(Select2Common.PLACEHOLDER)) {
                    hasPlaceholder = true;
                    // placeholder can be translated
                    if (isTranslated || Boolean.parseBoolean((String) propertySet.get("translatePlaceholder"))) {
                        value = messages.get(entry.getValue().toString());
                    }
                } else if (entry.getKey().equals(Select2Common.AJAX_RERENDER)) {
                    hasAjaxReRender = true;
                } else if (entry.getKey().equals(Select2Common.WIDTH)) {
                    hasWidth = true;
                } else if (entry.getKey().equals(Select2Common.MIN_CHARS)) {
                    hasMinChars = true;
                }

                jg.writeStringField(entry.getKey(), value);

            }

            if (defaultParams != null) {
                // All default params which are not in widget properties
                for (Entry<String, String> e : defaultParams.entrySet()) {
                    jg.writeStringField(e.getKey(), e.getValue());
                }
            }

            if (!hasPlaceholder) {
                // No placeholder provider and Select2 requires one to enable
                // the
                // reset button.
                jg.writeStringField(Select2Common.PLACEHOLDER, messages.get("label.vocabulary.selectValue"));
            }

            if (!hasWidth) {
                jg.writeStringField(Select2Common.WIDTH, Select2Common.DEFAULT_WIDTH);
            }

            if (!hasMinChars) {
                jg.writeNumberField(Select2Common.MIN_CHARS, Select2Common.DEFAULT_MIN_CHARS);
            }

            if (hasAjaxReRender) {
                jg.writeStringField(Select2Common.RERENDER_JS_FUNCTION_NAME, widget.getId() + "_reRender");
            }

            jg.writeEndObject();
            jg.flush();
            out.flush();
            return new String(baos.toByteArray(), "UTF-8");
        } catch (IOException e) {
            log.error("Could not encode parameters", e);
            return null;
        }
    }

    protected LayoutStore getLayoutStore() {
        return Framework.getService(LayoutStore.class);
    }

    @SuppressWarnings("rawtypes")
    protected JSONArray getMultipleDirectoryEntries(final Object value, final String directoryName,
            final boolean localize, String keySeparator, final boolean dbl10n, final String labelFieldName) {
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
            final String label = Select2Common.getLabelFieldName(schema, dbl10n, labelFieldName, locale.getLanguage());

            for (String ref : storedRefs) {
                JSONObject obj = resolveDirectoryEntry(ref, keySeparator, session, schema, label, localize, dbl10n);
                if (obj != null) {
                    result.add(obj);
                }
            }
            return result;
        } catch (DirectoryException de) {
            log.error("An error occured while obtaining directory " + directoryName, de);
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
    protected JSONArray getMultipleUserReference(final Object value, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
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
            JSONObject resolved = getSingleUserReference(ref, prefixed, firstLabelField, secondLabelField,
                    thirdLabelField, hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion,
                    hideIcon);
            if (resolved != null && !resolved.isEmpty()) {
                result.add(resolved);
            }
        }
        return result;
    }

    protected CoreSession getRepositorySession(String repoName) throws ClientException {

        if (repoName == null || repoName.isEmpty()) {
            RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
            repoName = rm.getDefaultRepositoryName();
        }

        if (documentManager != null && documentManager.getRepositoryName().equals(repoName)) {
            return documentManager;
        }

        dedicatedSession = CoreInstance.openCoreSession(repoName);
        return dedicatedSession;
    }

    protected JSONObject getSingleDirectoryEntry(final String storedReference, final String directoryName,
            final boolean localize, String keySeparator, final boolean dbl10n, final String labelFieldName) {

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
            final String label = Select2Common.getLabelFieldName(schema, dbl10n, labelFieldName, locale.getLanguage());

            JSONObject obj = resolveDirectoryEntry(storedReference, keySeparator, session, schema, label, localize,
                    dbl10n);

            return obj;
        } catch (DirectoryException de) {
            log.error("An error occured while obtaining directory " + directoryName, de);
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

    protected JSONObject getSingleUserReference(final String storedReference, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        JSONObject obj = new JSONObject();
        if (storedReference == null || storedReference.isEmpty()) {
            return null;
        }
        try {
            DocumentModel user = null;
            DocumentModel group = null;
            Directory userDir = dirService.getDirectory(userManager.getUserDirectoryName());
            if (prefixed) {
                if (storedReference.startsWith(NuxeoPrincipal.PREFIX)) {
                    user = userManager.getUserModel(storedReference.substring(NuxeoPrincipal.PREFIX.length()));
                } else if (storedReference.startsWith(NuxeoGroup.PREFIX)) {
                    group = userManager.getGroupModel(storedReference.substring(NuxeoGroup.PREFIX.length()));
                } else {
                    log.warn("User reference is prefixed but prefix was not found on reference: " + storedReference);
                    return null;
                }
            } else {
                user = userManager.getUserModel(storedReference);
                if (user == null) {
                    group = userManager.getGroupModel(storedReference);
                }
            }
            if (user != null) {
                Schema schema = schemaManager.getSchema(userManager.getUserSchemaName());
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = user.getPropertyValue(fieldName.getPrefixedName());
                    if (key.equals(userDir.getPasswordField())) {
                        continue;
                    }
                    obj.element(key, value);
                }
                String userId = user.getId();
                obj.put(Select2Common.ID, userId);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.USER_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + userId);
                Select2Common.computeUserLabel(obj, firstLabelField, secondLabelField, thirdLabelField, hideFirstLabel,
                        hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, userId);
                Select2Common.computeUserGroupIcon(obj, hideIcon);
            } else if (group != null) {
                Schema schema = schemaManager.getSchema(userManager.getGroupSchemaName());
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = group.getPropertyValue(fieldName.getPrefixedName());
                    obj.element(key, value);
                }
                // If the group hasn't an label, let's put the groupid
                String groupId = group.getId();
                Select2Common.computeGroupLabel(obj, groupId, userManager.getGroupLabelField(), hideFirstLabel);
                obj.put(Select2Common.ID, groupId);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.GROUP_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + groupId);
                Select2Common.computeUserGroupIcon(obj, hideIcon);
            } else {
                log.warn("Could not resolve user or group reference: " + storedReference);
                return null;
            }
        } catch (ClientException e) {
            log.error("An error occured while retrieving user or group reference: " + storedReference);
            return null;
        }
        return obj;
    }

    public boolean isMultiSelection(final Widget widget) {
        String wtCat = widget.getTypeCategory();
        if (StringUtils.isBlank(wtCat)) {
            wtCat = "jsf";
        }
        WidgetTypeDefinition wtDef = getLayoutStore().getWidgetTypeDefinition(wtCat, widget.getType());
        if (wtDef != null) {
            WidgetTypeConfiguration conf = wtDef.getConfiguration();
            if (conf != null) {
                return conf.isList();
            }
        }
        return false;
    }

    public boolean mustIncludeResources() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
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

    protected JSONObject createEntryWithWarnMessage(final String label, final String warnMessage) {
        JSONObject obj = new JSONObject();
        obj.put(Select2Common.ID, label);
        obj.put(Select2Common.ABSOLUTE_LABEL, label);
        obj.put(Select2Common.LABEL, label);
        obj.put(Select2Common.WARN_MESSAGE_LABEL, warnMessage);
        return obj;
    }

    protected JSONObject resolveDirectoryEntry(final String storedReference, String keySeparator,
            final Session session, final Schema schema, final String label, final boolean localize, final boolean dbl10n) {
        if (storedReference == null || storedReference.isEmpty()) {
            log.trace("No reference provided ");
            return null;
        }

        if (keySeparator == null || keySeparator.isEmpty()) {
            keySeparator = Select2Common.DEFAULT_KEY_SEPARATOR;
        }

        String entryId = storedReference.substring(storedReference.lastIndexOf(keySeparator) + 1,
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
                    if (localize && !dbl10n) {
                        value = messages.get(value);
                    }
                    obj.element(Select2Common.LABEL, value);
                    obj.element(Select2Common.ABSOLUTE_LABEL,
                            getParentAbsoluteLabel(storedReference, keySeparator, session, fieldName, localize, dbl10n));
                } else {
                    obj.element(key, value);
                }
            }

            // Add a warning message if the entity is obsolete
            if (obj.containsKey(Select2Common.OBSOLETE_FIELD_ID) && obj.getInt(Select2Common.OBSOLETE_FIELD_ID) > 0) {
                obj.element(Select2Common.WARN_MESSAGE_LABEL, messages.get("label.vocabulary.entry.obsolete"));
            }

            obj.element(Select2Common.COMPUTED_ID, storedReference);
            return obj;
        } catch (ClientException e) {
            log.error("An error occured while resolving directoryEntry", e);
            return null;
        }
    }

    /**
     * @since 5.9.3
     */
    protected String getParentAbsoluteLabel(final String entryId, final String keySeparator, final Session session,
            final QName labelFieldName, final boolean localize, final boolean dbl10n) throws PropertyException,
            ClientException {
        String[] split = entryId.split(keySeparator);
        String result = "";
        for (int i = 0; i < split.length; i++) {
            DocumentModel entry = session.getEntry(split[i]);
            if (entry != null) {
                Serializable value = entry.getPropertyValue(labelFieldName.getPrefixedName());
                if (localize && !dbl10n) {
                    value = messages.get(value);
                }
                result += (i > 0 ? "/" : "") + value;
            }
        }

        return result;
    }

    public String resolveMultipleDirectoryEntries(final Object value, final String directoryName,
            final boolean localize, String keySeparator, final boolean dbl10n, final String labelFieldName) {
        JSONArray result = getMultipleDirectoryEntries(value, directoryName, localize, keySeparator, dbl10n,
                labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "[]";
        }
    }

    public List<String> resolveMultipleDirectoryEntryLabels(final Object value, final String directoryName,
            final boolean localize, final String keySeparator, final boolean dbl10n, final String labelFieldName) {
        return formatList(getMultipleDirectoryEntries(value, directoryName, localize, keySeparator, dbl10n,
                labelFieldName));
    }

    @SuppressWarnings("rawtypes")
    public List<String> resolveMultipleReferenceLabels(final Object value, final String repo,
            final String operationName, final String idProperty, final String label) {

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
            DocumentModel doc = resolveReference(repo, ref, operationName, idProperty);
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
            } else {
                result.add(messages.get("label.documentSuggestion.docNotFoundOrNotVisible") + "(" + ref + ")");
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public String resolveMultipleReferences(final Object value, final String repo, final String operationName,
            final String idProperty, final String schemaNames) throws IOException {

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
        if (storedRefs.isEmpty()) {
            return "[]";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        jg.writeStartArray();

        DocumentModelJsonWriter writer = getDocumentModelWriter(schemaNames);

        for (String ref : storedRefs) {
            DocumentModel doc = resolveReference(repo, ref, operationName, idProperty);
            if (doc == null) {
                processDocumentNotFound(ref, jg);
            } else {
                writer.write(doc, jg);
            }
        }

        jg.writeEndArray();
        out.flush();
        String json = new String(baos.toByteArray(), "UTF-8");

        if (json.isEmpty()) {
            return "[]";
        }
        if (json.startsWith("[") && !json.endsWith("]")) {
            // XXX !!!
            // AT: what's this for?
            json = json + "]";
        }

        return json;
    }

    @SuppressWarnings("rawtypes")
    public String resolveMultipleUserReference(final Object value, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
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
            String resolved = resolveSingleUserReference(ref, prefixed, firstLabelField, secondLabelField,
                    thirdLabelField, hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion,
                    hideIcon);
            if (resolved != null && !resolved.isEmpty()) {
                result.add(resolved);
            }
        }
        return result.toString();
    }

    public List<String> resolveMultipleUserReferenceLabels(final Object value, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
        return formatList(getMultipleUserReference(value, prefixed, firstLabelField, secondLabelField, thirdLabelField,
                hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, hideIcon));
    }

    protected DocumentModel resolveReference(final String repo, final String storedReference,
            final String operationName, final String idProperty) {

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
                    String query = " select * from Document where " + idProperty + "='" + storedReference + "'";
                    DocumentModelList docs = session.query(query);
                    if (docs.size() > 0) {
                        return docs.get(0);
                    } else {
                        log.warn("Unable to resolve doc using property " + idProperty + " and value " + storedReference);
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
                Map<String, Object> params = new HashMap<String, Object>();

                params.put("value", storedReference);
                params.put("xpath", idProperty);
                params.put("lang", org.jboss.seam.core.Locale.instance().getLanguage());
                Object result = as.run(ctx, operationName, params);

                if (result == null) {
                    log.warn("Unable to resolve reference " + storedReference + " using property " + idProperty
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

    protected void processDocumentNotFound(String id, JsonGenerator jg) throws ClientException {
        if (StringUtils.isEmpty(id)) {
            return;
        }
        try {
            jg.writeStartObject();
            jg.writeStringField(Select2Common.ID, id);
            jg.writeStringField(Select2Common.TITLE, messages.get("label.documentSuggestion.docNotFoundOrNotVisible"));
            jg.writeStringField(Select2Common.WARN_MESSAGE_LABEL, id);
            jg.writeEndObject();
            jg.flush();
        } catch (IOException e) {
            log.error("Error while writing not found message ", e);
        }

    }

    public String resolveSingleDirectoryEntry(final String storedReference, final String directoryName,
            final boolean localize, String keySeparator, final boolean dbl10n, final String labelFieldName) {
        JSONObject result = getSingleDirectoryEntry(storedReference, directoryName, localize, keySeparator, dbl10n,
                labelFieldName);
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    public String resolveSingleDirectoryEntryLabel(final String storedReference, final String directoryName,
            final boolean localize, String keySeparator, final boolean dbl10n, final String labelFieldName) {
        JSONObject obj = getSingleDirectoryEntry(storedReference, directoryName, localize, keySeparator, dbl10n,
                labelFieldName);
        if (obj == null) {
            return "";
        }
        return obj.optString(Select2Common.LABEL);
    }

    public String resolveSingleReference(final String storedReference, final String repo, final String operationName,
            final String idProperty, final String schemaNames) throws IOException {

        DocumentModel doc;
        doc = resolveReference(repo, storedReference, operationName, idProperty);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);
        JsonGenerator jg = JsonHelper.createJsonGenerator(out);
        if (doc == null) {
            processDocumentNotFound(storedReference, jg);
        } else {
            getDocumentModelWriter(schemaNames).write(doc, jg);
        }
        jg.flush();
        return new String(baos.toByteArray(), "UTF-8");

    }

    public String resolveSingleReferenceLabel(final String storedReference, final String repo,
            final String operationName, final String idProperty, final String label) {
        DocumentModel doc = resolveReference(repo, storedReference, operationName, idProperty);
        if (doc == null) {
            return messages.get("label.documentSuggestion.docNotFoundOrNotVisible") + "(" + doc + ")";
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

    public String resolveSingleUserReference(final String storedReference, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
        JSONObject result = getSingleUserReference(storedReference, prefixed, firstLabelField, secondLabelField,
                thirdLabelField, hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, hideIcon);
        if (result != null) {
            return result.toString();
        } else {
            return "";
        }
    }

    public String resolveUserReferenceLabel(final String storedReference, final boolean prefixed,
            final String firstLabelField, final String secondLabelField, final String thirdLabelField,
            final boolean hideFirstLabel, final boolean hideSecondLabel, final boolean hideThirdLabel,
            final boolean displayEmailInSuggestion, final boolean hideIcon) {
        JSONObject obj = getSingleUserReference(storedReference, prefixed, firstLabelField, secondLabelField,
                thirdLabelField, hideFirstLabel, hideSecondLabel, hideThirdLabel, displayEmailInSuggestion, hideIcon);
        if (obj == null) {
            return "";
        }
        return obj.optString(Select2Common.LABEL);
    }

    protected DocumentModelJsonWriter getDocumentModelWriter(final String schemaNames) {
        MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
        String[] schemas = Select2Common.getSchemas(schemaNames);
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        RenderingContext ctx = RenderingContextWebUtils.getBuilder(request).properties(schemas).enrichDoc("documentURL").get();
        return registry.getInstance(ctx, DocumentModelJsonWriter.class);
    }

}
