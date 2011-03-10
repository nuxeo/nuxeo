/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.STATELESS;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;


/**
 * Performs re-rendering of webcontainer layout widgets.
 *
 * @author Anahide Tchertchian
 * @author rux added the site name validation
 */
@Name("siteActions")
@Scope(STATELESS)
public class SiteActionsBean {

    private static final Log log = LogFactory.getLog(SiteActionsBean.class);

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(required = true, create = true)
    protected transient NavigationContext navigationContext;

    protected static final String WEBSITE = "WebSite";

    protected static final String WEBBLOG = "BlogSite";

    /**
     * Validates the web container fields. If the workspace is web container, it
     * also needs to have name. The usual required JSF component can't be used,
     * because it will block the validation no matter if the checkbox is set or
     * not. As result, the widget validation is used. The both values need to be
     * available in layout to be used.
     */
    public void validateName(FacesContext context, UIComponent component,
            Object value) {

        Map<String, Object> attributes = component.getAttributes();

        String wcId = (String) attributes.get("webContainerId");
        if (wcId == null) {
            log.debug("Cannot validate name: input wcId not found");
            return;
        }

        UIInput wcComp = (UIInput) component.findComponent(wcId);
        if (wcComp == null) {
            log.debug("Cannot validate name: input wcId not found second time");
            return;
        }

        Boolean propValue = (Boolean) wcComp.getLocalValue();
        boolean isWC = false;
        if (propValue != null) {
            isWC = propValue;
        }
        if (!isWC) {
            // no need validation if not web container
            return;
        }

        String nameId = (String) attributes.get("nameId");
        if (nameId == null) {
            log.error("Cannot validate name: input id(s) not found");
            return;
        }

        UIInput nameComp = (UIInput) component.findComponent(nameId);
        if (nameComp == null) {
            log.error("Cannot validate name: input(s) not found second time");
            return;
        }

        Object nameObj = nameComp.getLocalValue();

        if (nameObj == null || StringUtils.isBlank(nameObj.toString())) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.error.need.name.webcontainer"),
                            null);
            throw new ValidatorException(message);
        }

    }

    public void validateSiteTitle(FacesContext context, UIComponent component,
            Object value) {
        validateSite(context, component, value, WEBSITE);
    }

    public void validateBlogTitle(FacesContext context, UIComponent component,
            Object value) {
        validateSite(context, component, value, WEBBLOG);
    }

    private void validateSite(FacesContext context, UIComponent component,
            Object value, String siteType) {
        if (value instanceof String) {
            try {
                // apply Title2Path translation
                PathSegmentService pss;
                try {
                    pss = Framework.getService(PathSegmentService.class);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                DocumentModel fakeDoc = documentManager.createDocumentModel(siteType);
                fakeDoc.setPropertyValue("dc:title", (String) value);
                String name = pss.generatePathSegment(fakeDoc);

                DocumentModelList sites = querySitesByUrlAndDocType(
                        documentManager, name, siteType);
                // if editing a site don't verify it's unique against itself
                DocumentModel currentDocument = navigationContext.getCurrentDocument();
                if (siteType.equals(currentDocument.getType())) {
                    sites.remove(currentDocument);
                }
                if (!sites.isEmpty()) {
                    FacesMessage message = new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            ComponentUtils.translate(context, "label.site.notunique.title"),
                            null);
                    // also add global message
                    context.addMessage(null, message);
                    throw new ValidatorException(message);
                }
            } catch (ClientException e) {
                log.error(e);
            }
        }
    }

    private DocumentModelList querySitesByUrlAndDocType(CoreSession session,
            String url, String documentType) throws ClientException {
        QuerySitesUnrestricted unrestrictedRunner = new QuerySitesUnrestricted(
                session, documentType, url);
        unrestrictedRunner.runUnrestricted();
        return unrestrictedRunner.getResultList();
    }

    private class QuerySitesUnrestricted extends UnrestrictedSessionRunner {

        private final String documentType;

        private final String url;

        private DocumentModelList list;

        public QuerySitesUnrestricted(CoreSession session, String documentType,
                String url) {
            super(session);
            this.documentType = documentType;
            this.url = url;

        }

        @Override
        public void run() throws ClientException {
            String queryString = String.format("SELECT * FROM %s WHERE "
                    + "ecm:mixinType = 'WebView' AND webc:url = \"%s\" AND "
                    + "ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0 "
                    + "AND ecm:currentLifeCycleState != 'deleted' "
                    + "AND webc:isWebContainer = 1", documentType, url);
            list = session.query(queryString);
        }

        DocumentModelList getResultList() {
            return list;
        }
    }

}
