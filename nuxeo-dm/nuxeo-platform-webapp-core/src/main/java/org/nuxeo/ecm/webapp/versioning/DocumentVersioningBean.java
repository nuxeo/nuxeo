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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

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
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
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

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    /**
     * @deprecated since 5.7.3: versioning options map does not need to be kept
     *             in cache anymore here
     */
    @Deprecated
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

    @Override
    public Collection<VersionModel> getItemVersioningHistory(
            DocumentModel document) {
        List<VersionModel> versions = Collections.emptyList();
        try {
            versions = documentManager.getVersionsForDocument(document.getRef());
            for (VersionModel model : versions) {
                DocumentModel ver = documentManager.getDocumentWithVersion(
                        document.getRef(), model);
                if (ver != null) {
                    model.setDescription(ver.getAdapter(
                            VersioningDocument.class).getVersionLabel());
                }
            }
        } catch (ClientException e) {
            log.error("Error retrieving versioning history ", e);
        }
        return versions;
    }

    @Override
    public Collection<VersionModel> getCurrentItemVersioningHistory() {
        return getItemVersioningHistory(navigationContext.getCurrentDocument());
    }

    @Factory(autoCreate = true, value = "currentDocumentVersionInfo", scope = EVENT)
    public VersionInfo getCurrentDocumentVersionInfo() throws ClientException {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        String versionLabel = versioningManager.getVersionLabel(doc);
        boolean available = versionLabel != null && versionLabel.length() != 0;
        return new VersionInfo(versionLabel, available);
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void resetVersioningOption() {
        availableVersioningOptionsMap = null;
        selectedOption = null;
        rendered = null;
    }

    @Deprecated
    @Override
    public Map<String, String> getAvailableVersioningOptionsMap() {
        // FIXME: should cache the map and invalidate it correctly as it refers
        // to current document
        DocumentModel doc = navigationContext.getCurrentDocument();
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Entry<String, String> en : getVersioningOptionsMap(doc).entrySet()) {
            // reverse keys with values for the jsf controls
            map.put(en.getValue(), en.getKey());
        }
        availableVersioningOptionsMap = map;
        return availableVersioningOptionsMap;
    }

    @Override
    public Map<String, String> getVersioningOptionsMap(DocumentModel doc) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        VersionIncEditOptions options = getAvailableVersioningOptions(doc);
        if (options != null) {
            for (VersioningActions option : options.getOptions()) {
                String label = "label.versioning.option." + option.toString();
                if (resourcesAccessor != null) {
                    label = resourcesAccessor.getMessages().get(label);
                }
                map.put(option.name(), label);
            }
        }
        return map;
    }

    /**
     * @deprecated since 5.7.3: options are resolved from the layout value
     *             instead now that a generic widget definition handles it
     */
    @Deprecated
    private VersionIncEditOptions getCurrentAvailableVersioningOptions() {
        return getAvailableVersioningOptions(navigationContext.getCurrentDocument());
    }

    public VersionIncEditOptions getAvailableVersioningOptions(
            DocumentModel doc) {
        try {
            return versioningManager.getVersionIncEditOptions(doc);
        } catch (ClientException e) {
            log.error("Error retrieving versioning options ", e);
            return null;
        }
    }

    @Override
    public String getVersionLabel(DocumentModel doc) throws ClientException {
        return versioningManager.getVersionLabel(doc);
    }

    @Deprecated
    @Override
    public String getVersioningOptionInstanceId() {
        if (selectedOption == null) {
            // FIXME: should cache the versioning options and invalidate them
            // correctly as it refers to current document
            VersionIncEditOptions options = getCurrentAvailableVersioningOptions();
            if (options != null) {
                selectedOption = options.getDefaultVersioningAction();
            }
            if (selectedOption == null) {
                selectedOption = VersioningActions.ACTION_NO_INCREMENT;
            }
        }
        return selectedOption.name();
    }

    @Deprecated
    @Override
    public void setVersioningOptionInstanceId(String optionId)
            throws ClientException {
        setVersioningOptionInstanceId(navigationContext.getCurrentDocument(),
                optionId);
    }

    @Deprecated
    @Override
    public void setVersioningOptionInstanceId(DocumentModel docModel,
            String optionId) throws ClientException {
        if (optionId != null) {
            selectedOption = VersioningActions.valueOf(optionId);
            setVersioningOptionInstanceId(docModel, selectedOption);
        } else {
            // component is present but no option has been selected
            // should not reach here...
        }
    }

    @Deprecated
    @Override
    public void setVersioningOptionInstanceId(DocumentModel docModel,
            VersioningActions option) throws ClientException {
        // add version inc option to document context so it will be
        // taken into consideration on the server side
        VersioningOption vo;
        if (option == VersioningActions.ACTION_INCREMENT_MAJOR) {
            vo = VersioningOption.MAJOR;
        } else if (option == VersioningActions.ACTION_INCREMENT_MINOR) {
            vo = VersioningOption.MINOR;
        } else if (option == VersioningActions.ACTION_NO_INCREMENT) {
            vo = VersioningOption.NONE;
        } else {
            vo = null;
        }
        docModel.putContextData(VersioningService.VERSIONING_OPTION, vo);
    }

    @Override
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

    @Deprecated
    @Override
    @Factory(value = "renderVersioningOptionsForCurrentDocument", scope = EVENT)
    public boolean factoryForRenderVersioningOption() {
        return getRendered();
    }

    /**
     * @deprecated since 5.7.3: rendered clause is now evaluated on the widget
     *             definition instead
     */
    @Deprecated
    public boolean getRendered() {
        if (rendered == null) {
            rendered = Boolean.FALSE;
            if (navigationContext.getCurrentDocument() != null) {
                Map<String, String> options = getAvailableVersioningOptionsMap();
                // do not display the versioning options if there is only one
                // choice
                if (options != null && options.size() > 1) {
                    rendered = Boolean.TRUE;
                }
            }
        }
        return rendered.booleanValue();
    }

    /**
     * @deprecated since 5.7.3: rendered clause is now evaluated on the widget
     *             definition instead
     */
    @Deprecated
    public void setRendered(Boolean rendered) {
        this.rendered = rendered;
    }

}
