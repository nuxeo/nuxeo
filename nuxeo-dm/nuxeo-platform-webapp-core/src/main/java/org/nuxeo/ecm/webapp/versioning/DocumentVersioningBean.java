/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Web action bean for document versioning. Used also by other seam components through injection.
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

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    private transient VersioningManager versioningManager;

    /**
     * field used for deciding whether or not to display versioning controls section (in document editing)
     */
    private Boolean rendered;

    private VersioningActions selectedOption;

    @Override
    public Collection<VersionModel> getItemVersioningHistory(DocumentModel document) {
        List<VersionModel> versions = Collections.emptyList();
        versions = documentManager.getVersionsForDocument(document.getRef());
        for (VersionModel model : versions) {
            DocumentModel ver = documentManager.getDocumentWithVersion(document.getRef(), model);
            if (ver != null) {
                model.setDescription(ver.getAdapter(VersioningDocument.class).getVersionLabel());
            }
        }
        return versions;
    }

    @Override
    public Collection<VersionModel> getCurrentItemVersioningHistory() {
        return getItemVersioningHistory(navigationContext.getCurrentDocument());
    }

    @Factory(autoCreate = true, value = "currentDocumentVersionInfo", scope = EVENT)
    public VersionInfo getCurrentDocumentVersionInfo() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        String versionLabel = versioningManager.getVersionLabel(doc);
        boolean available = versionLabel != null && versionLabel.length() != 0;
        return new VersionInfo(versionLabel, available);
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void resetVersioningOption() {
        selectedOption = null;
        rendered = null;
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

    public VersionIncEditOptions getAvailableVersioningOptions(DocumentModel doc) {
        return versioningManager.getVersionIncEditOptions(doc);
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return versioningManager.getVersionLabel(doc);
    }

    @Override
    public void validateOptionSelection(FacesContext context, UIComponent component, Object value) {
        if (value != null) {
            // ok
            return;
        }
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        String msg = I18NUtils.getMessageString(bundleName, "error.versioning.none_selected", null, locale);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);

        throw new ValidatorException(message);
    }

}
