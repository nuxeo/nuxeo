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

package org.nuxeo.webengine.sites;

import static org.nuxeo.webengine.sites.utils.SiteConstants.DEFAULT_WEBSITE_THEMEPAGE_VALUE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.DEFAULT_WEBSITE_THEME_VALUE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_THEME_PAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE_SCHEMA_THEME;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE_THEMEPAGE;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;

/**
 * Web object implementation corresponding to Site. It is resolved from module
 * root web object. It holds the site fragments back methods.
 */
@WebObject(type = WEBSITE, superType = "AbstractSiteDocumentObject", facets = { WEBSITE })
@Produces("text/html;charset=UTF-8")
public class Site extends AbstractSiteDocumentObject {

    private static final Log log = LogFactory.getLog(Site.class);

    protected String url;

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        url = (String) args[0];
        doc = getSiteDocumentModelByUrl(url);
    }

    /**
     * Must use the same method name as super.traverse() otherwise we introduce
     * ambiguous sub-resources locators which will fail in jersey.
     */
    @Path("{page}")
    public Resource traverse(@PathParam("page") String page) {
        try {
            DocumentModel pageDoc = ctx.getCoreSession().getChild(doc.getRef(),
                    page);
            setDoGetParameters();
            return ctx.newObject(pageDoc.getType(), pageDoc);
        } catch (Exception e) {
            return super.traverse(page);
        }
    }

    protected DocumentModel getSiteDocumentModelByUrl(String url) {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            DocumentModelList list = SiteQueriesCollection.querySitesByUrlAndDocType(
                    session, url, getWebSiteDocumentType());
            if (!list.isEmpty()) {
                return list.get(0);
            }
            if (((NuxeoPrincipal) session.getPrincipal()).isAnonymous()) {
                Boolean siteExists = SiteQueriesCollection.checkUnrestrictedSiteExistenceByUrlAndDocType(
                        session, url, getWebSiteDocumentType());
                if (siteExists) {
                    setForceRedirectToLogout(true);
                }
            }
        } catch (ClientException e) {
            log.error("Unable to retrieve the webcontainer ", e);
        }
        return null;
    }

    @Override
    protected String getSchemaFieldThemeName() {
        return WEBSITE_SCHEMA_THEME;
    }

    @Override
    protected String getDefaultSchemaFieldThemeValue() {
        return DEFAULT_WEBSITE_THEME_VALUE;
    }

    @Override
    protected String getSchemaFieldThemePageName() {
        return WEBSITE_THEMEPAGE;
    }

    @Override
    protected String getDefaultSchemaFieldThemePageValue() {
        return DEFAULT_WEBSITE_THEMEPAGE_VALUE;
    }

    @Override
    protected Map<String, Object> getErrorArguments() {
        Map<String, Object> errorArguments = new HashMap<String, Object>();
        errorArguments.put("url", url);
        return errorArguments;
    }

    @Override
    protected String getErrorTemplateName() {
        return "no_site.ftl";
    }

    @Override
    protected String getDocumentDeletedErrorTemplateName() {
        return "no_site.ftl";
    }

    @Override
    protected String getSearchThemePage() {
        return SEARCH_THEME_PAGE;
    }
}
