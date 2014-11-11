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
package org.nuxeo.ecm.webapp.versioning;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Web action bean for document versioning. Used also by other seam components
 * through injection.
 *
 * @author Dragos Mihalache
 */
@Name("documentVersioning")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DocumentVersioningBean implements DocumentVersioning, Serializable {

    private static final long serialVersionUID = 75409841629876L;

    private static final Log log = LogFactory.getLog(DocumentVersioningBean.class);

    private static final String VER_INFO_AUTO_INC_KEY = "message.versioning.editoptionsinfo.auto_increment";
    private static final String VER_INFO_NO_INC_KEY = "message.versioning.editoptionsinfo.no_increment";
    private static final String VER_INFO_UNDEF_KEY = "message.versioning.editoptionsinfo.undefined";

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected Map<String, String> availableVersioningOptionsMap;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    private transient VersioningManager versioningManager;

    /**
     * field used for deciding whether or not to display versioning controls
     * section (in document editing)
     */
    private Boolean rendered;

    private VersioningActions selectedOption;

    // XXX: cache to workaround a performance problem computing availability of
    // versioning schema we should probably use a lighter test such as a trusted
    // facet instead of remote calls to he versioning service and schema manager
    private final Map<String, Boolean> uidInfoAvailableCache = new HashMap<String, Boolean>();

    public Collection<VersioningActions> getCurrentItemVersioningOptions() {
        final VersionIncEditOptions options = getAvailableVersioningOptions(navigationContext.getCurrentDocument());

        if (null == options) {
            return Collections.emptyList();
        }

        return options.getOptions();
    }

    public Collection<VersionModel> getItemVersioningHistory(
            DocumentModel document) {
        return getAvailableVersioningHistory(document);
    }

    // FIXME: should cache the list and invalidate it correctly as it refers to
    // current document
    public Collection<VersionModel> getCurrentItemVersioningHistory() {
        return getItemVersioningHistory(navigationContext.getCurrentDocument());
    }

    @Factory(value = "currentDocumentIncrementationRules", scope = EVENT)
    // FIXME: should cache the String and invalidate it correctly as it refers
    // to current document
    public String factoryForIncrementationRules() {
        if (!getRendered()) {
            return null;
        }
        return getIncRulesResult();
    }

    @Factory(autoCreate = true, value = "currentDocumentVersionInfo", scope = EVENT)
    public VersionInfo getCurrentDocumentVersionInfo() throws ClientException {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        return new VersionInfo(getVersionLabel(docModel), getUidInfoAvailable());
    }

    /**
     * Get incrementation rules text info. If this is null, inc options for user
     * selection could be rendered. Otherwise this info could be shown to the
     * user as an explanation of what versioning rule will be automatically
     * applied.
     */
    // FIXME: should cache the String and invalidate it correctly as it refers
    // to current document
    public String getIncRulesResult() {
        String editOptionsInfo = null;

        // make one call only => Factory
        final VersionIncEditOptions options = getAvailableVersioningOptions(
                navigationContext.getCurrentDocument());

        if (null == options) {
            editOptionsInfo = "Error retrieving inc options.";
            return editOptionsInfo;
        }

        final VersioningActions vAction = options.getVersioningAction();
        if (vAction == VersioningActions.ACTION_AUTO_INCREMENT) {
            editOptionsInfo = resourcesAccessor.getMessages().get(
                    VER_INFO_AUTO_INC_KEY);
        } else if (vAction == VersioningActions.ACTION_NO_INCREMENT) {
            editOptionsInfo = resourcesAccessor.getMessages().get(
                    VER_INFO_NO_INC_KEY);
        } else if (vAction == VersioningActions.ACTION_UNDEFINED) {
            editOptionsInfo = resourcesAccessor.getMessages().get(
                    VER_INFO_UNDEF_KEY);
        } else if (vAction == VersioningActions.NO_VERSIONING) {
            editOptionsInfo = null;
        } else if (vAction == null) {
            editOptionsInfo = "please review versioning rules: "
                    + options.getInfo();
        } else {
            // options will be presented to the user
            if (options.getOptions().isEmpty()) {
                editOptionsInfo = "Error. option: " + vAction + "; info: "
                        + options.getInfo();
            }
        }

        return editOptionsInfo;
    }

    /**
     *
     * @param doc
     * @return collection with versions of the given doc model or an empty list
     */
    private Collection<VersionModel> getAvailableVersioningHistory(
            DocumentModel doc) {
        List<VersionModel> versions = Collections.emptyList();

        try {
            versions = documentManager.getVersionsForDocument(doc.getRef());

            for (VersionModel model : versions) {
                DocumentModel tempDoc = documentManager.getDocumentWithVersion(
                        doc.getRef(), model);
                if (tempDoc != null) {
                    VersioningDocument docVer = tempDoc.getAdapter(VersioningDocument.class);
                    String minorVer = docVer.getMinorVersion().toString();
                    String majorVer = docVer.getMajorVersion().toString();
                    model.setDescription(majorVer + '.' + minorVer);
                }
            }

        } catch (ClientException e) {
            log.error("Error retrieving versioning history ", e);
        } catch (DocumentException e) {
            log.error("Error retrieving versioning history ", e);
        }
        return versions;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetVersioningOption() {
        availableVersioningOptionsMap = null;
        selectedOption = null;
        rendered = null;
    }

    // FIXME: should cache the map and invalidate it correctly as it refers to
    // current document
    public Map<String, String> getAvailableVersioningOptionsMap() {
        final String logPrefix = "<getAvailableVersioningOptionsMap> ";

        log.debug(logPrefix + "Recomputing versioning options list");

        DocumentModel docModel = navigationContext.getCurrentDocument();
        final Map<String, String> versioningOptionsMap = getVersioningOptionsMap(docModel);

        // should reverse keys with values for the jsf controls
        availableVersioningOptionsMap = new LinkedHashMap<String, String>();
        for (String key : versioningOptionsMap.keySet()) {
            final String value = versioningOptionsMap.get(key);
            availableVersioningOptionsMap.put(value, key);
        }

        return availableVersioningOptionsMap;
    }

    /**
     * For documents about to be created there should be no versioning options.
     */
    @Observer(value = { EventNames.NEW_DOCUMENT_CREATED }, create = false)
    @BypassInterceptors
    public void resetRenderingStatus() {
        rendered = false;
    }

    public Map<String, String> getVersioningOptionsMap(DocumentModel documentModel) {

        if (documentModel == null) {
            throw new IllegalArgumentException("null documentModel");
        }

        final Map<String, String> versioningOptionsMap = new LinkedHashMap<String, String>();

        final VersionIncEditOptions options = getAvailableVersioningOptions(documentModel);

        if (options == null) {
            return versioningOptionsMap;
        }

        for (VersioningActions option : options.getOptions()) {
            final String optionResName = "label.versioning.option."
                    + option.toString();
            String label = optionResName;
            if (resourcesAccessor!=null) {
                label = resourcesAccessor.getMessages().get(optionResName);
            }
            versioningOptionsMap.put(option.name(), label);
        }

        return versioningOptionsMap;
    }

    /**
     * Gets available versioning options from the server for the given
     * DocumentModel.
     *
     * @param documentModel
     * @return <code>null</code> if errors encountered while retrieving
     *         options from the server
     */
    private VersionIncEditOptions getAvailableVersioningOptions(
            final DocumentModel documentModel) {

        VersionIncEditOptions options = null;
        try {
            options = versioningManager.getVersionIncEditOptions(documentModel);
        } catch (ClientException e) {
            log.error("Error retrieving versioning options ", e);
        }

        log.debug("Available options (retrieved from server): " + options);
        return options;
    }

    public String getVersionLabel(DocumentModel document)
            throws ClientException {
        return versioningManager.getVersionLabel(document);
    }

    @Deprecated
    public void incrementVersions(DocumentModel documentModel,
            VersioningActions selectedOption) {
        final String logPrefix = "<incrementVersions> ";
        log.debug(logPrefix + selectedOption);

        // save as new version
        try {
            saveDocumentAsNewVersion(documentModel);
        } catch (ClientException e) {
            log.error(e);
        }

        try {
            if (selectedOption == VersioningActions.ACTION_INCREMENT_MAJOR) {
                incrementMajor(documentModel);
            } else if (selectedOption == VersioningActions.ACTION_INCREMENT_MINOR) {
                incrementMinor(documentModel);
            }
            // no incrementation
        } catch (ClientException e) {
            log.error(e);
        }
    }

    /**
     *
     * @param documentModel
     * @return the version for the checked-in document
     * @throws ClientException
     */
    private VersionModel saveDocumentAsNewVersion(DocumentModel documentModel)
            throws ClientException {
        // Do a checkin / checkout of the edited version
        DocumentRef docRef = documentModel.getRef();
        VersionModel newVersion = new VersionModelImpl();
        newVersion.setLabel(documentManager.generateVersionLabelFor(docRef));
        documentManager.checkIn(docRef, newVersion);
        log.debug("Checked in " + documentModel);
        documentManager.checkOut(docRef);
        log.debug("Checked out " + documentModel);
        return newVersion;
    }

    public String getVersioningOptionInstanceId() {
        if (selectedOption == null) {
            // FIXME: should cache the versioning options and invalidate them
            // correctly as it refers to current document
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            final VersionIncEditOptions options = getAvailableVersioningOptions(currentDoc);
            // get it from options
            selectedOption = options.getDefaultVersioningAction();
            if (selectedOption == null) {
                // XXX default selected value, can be argued.
                selectedOption = VersioningActions.ACTION_NO_INCREMENT;
            }
        }
        return selectedOption.name();
    }

    public void setVersioningOptionInstanceId(String optionId)
            throws ClientException {
        setVersioningOptionInstanceId(navigationContext.getCurrentDocument(),
                optionId);
    }

    public void setVersioningOptionInstanceId(DocumentModel docModel,
            String optionId) throws ClientException {
        log.debug("selected option: " + optionId);
        if (optionId != null) {
            selectedOption = VersioningActions.valueOf(optionId);
            setVersioningOptionInstanceId(docModel, selectedOption);
        } else {
            // component is present but no option has been selected
            // should not reach here...
        }
    }

    /**
     * Set incrementation option for the given document.
     */
    public void setVersioningOptionInstanceId(DocumentModel docModel,
            VersioningActions option) throws ClientException {

        boolean evaluateCreateSnapshot = !getDisplayCreateSnapshotOption();
        setVersioningOptionInstanceId(docModel, option, evaluateCreateSnapshot);
    }

    public static void setVersioningOptionInstanceId(DocumentModel docModel,
            VersioningActions option, boolean evaluateCreateSnapshot) {

        // add version inc option to document context so it will be
        // taken into consideration on the server side
        docModel.putContextData(ScopeType.REQUEST,
                VersioningActions.KEY_FOR_INC_OPTION, option);

        // TODO make this configurable
        if (evaluateCreateSnapshot) {
            // option is not displayed so take default action...

            if (option == VersioningActions.ACTION_INCREMENT_MAJOR
                    || option == VersioningActions.ACTION_INCREMENT_MINOR) {
                docModel.putContextData(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
            }
        }
    }

    public void validateOptionSelection(FacesContext context,
            UIComponent component, Object value) {
        if (value != null) {
            // ok
            return;
        }
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        String msg = I18NUtils.getMessageString(bundleName,
                "error.versioning.none_selected", null, locale);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                msg, msg);

        throw new ValidatorException(message);
    }

    public void setCreateSnapshot(boolean createSnapshot) {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        docModel.putContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, createSnapshot);
    }

    public boolean getCreateSnapshot() throws ClientException {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        Object ctxCreateSnapshot = docModel.getContextData(ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY);

        boolean createSnapshot;
        if (ctxCreateSnapshot == null) {
            // not set, will return default
            createSnapshot = getDefaultCreateSnapshot();
        } else {
            createSnapshot = (Boolean) ctxCreateSnapshot;
        }

        return createSnapshot;
    }

    public boolean getDefaultCreateSnapshot() throws ClientException {
        // check with versioning rules
        DocumentModel docModel = navigationContext.getCurrentDocument();
        SnapshotOptions option = versioningManager.getCreateSnapshotOption(docModel);

        return option == SnapshotOptions.DISPLAY_SELECTED;
    }

    public boolean getDisplayCreateSnapshotOption() throws ClientException {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        SnapshotOptions option = versioningManager.getCreateSnapshotOption(docModel);

        return option == SnapshotOptions.DISPLAY_SELECTED
                || option == SnapshotOptions.DISPLAY_NOT_SELECTED;
    }

    @Factory(value = "renderVersioningOptionsForCurrentDocument", scope = EVENT)
    public boolean factoryForRenderVersioningOption() {
        return getRendered();
    }

    // FIXME: should cache the boolean invalidate it correctly as it refers to
    // current document
    public Boolean getRendered() {
        if (rendered == null) {
            rendered = false;
            if (navigationContext.getCurrentDocument() != null) {
                Map<String, String> options = getAvailableVersioningOptionsMap();
                if (options != null && !options.isEmpty()) {
                    rendered = true;
                }
            }
        }
        return rendered;
    }

    public void setRendered(Boolean rendered) {
        this.rendered = rendered;
    }

    /**
     * Tells whether or not the current object has uid schema (ie the versioning
     * info is available).
     */
    public Boolean getUidInfoAvailable() {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        String typeName = docModel.getType();
        Boolean isAvailable = uidInfoAvailableCache.get(typeName);
        if (isAvailable == null) {
            // XXX AT: this is a hack
            String majorProp = versioningManager.getMajorVersionPropertyName(typeName);
            String schemaName = DocumentModelUtils.getSchemaName(majorProp);
            try {
                isAvailable = docModel.getDataModel(schemaName) != null;
            } catch (ClientException e) {
                isAvailable = false;
            }
            uidInfoAvailableCache.put(typeName, isAvailable);
        }
        return isAvailable;
    }

    /**
     * Tells wether or not the current object has versions.
     */
    public Boolean getUidDataAvailable() {
        DocumentModel docModel = navigationContext.getCurrentDocument();
        // XXX AT: this is a hack
        String majorProp = versioningManager.getMajorVersionPropertyName(docModel.getType());
        String schemaName = DocumentModelUtils.getSchemaName(majorProp);
        String fieldName = DocumentModelUtils.getFieldName(majorProp);
        try {
            return docModel.getProperty(schemaName, fieldName) != null;
        } catch (ClientException e) {
            return null;
        }
    }

    /**
     * Direct action onto document.
     */
    @Deprecated
    public void incrementMajor(DocumentModel doc) throws ClientException {
        versioningManager.incrementMajor(doc);
    }

    @Deprecated
    public void incrementMinor(DocumentModel doc) throws ClientException {
        versioningManager.incrementMinor(doc);
    }

}
