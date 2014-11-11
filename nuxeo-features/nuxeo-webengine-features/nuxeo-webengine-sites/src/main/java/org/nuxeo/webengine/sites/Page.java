/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     stan
 */

package org.nuxeo.webengine.sites;

import static org.nuxeo.webengine.sites.utils.SiteConstants.DEFAULT_WEBPAGE_THEMEPAGE_VALUE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.DEFAULT_WEBPAGE_THEME_VALUE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_THEME_PAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_CONTENT;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_EDITOR;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_PUSHTOMENU;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_SCHEMA_THEME;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_THEMEPAGE;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.sites.utils.ContextTransmitterHelper;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Web object implementation corresponding to WebPage. It is resolved from site.
 * It holds the web page fragments back methods.
 *
 * @author stan
 */
@WebObject(type = WEBPAGE, superType = "AbstractSiteDocumentObject")
@Produces("text/html;charset=UTF-8")
public class Page extends AbstractSiteDocumentObject {

    private static final Log log = LogFactory.getLog(Page.class);

    protected String pathSegment;

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        Object arg = args[0];
        if (arg instanceof String) {
            pathSegment = (String) arg;
        } else {
            doc = (DocumentModel) arg;
        }
    }

    @Override
    @POST
    public Response doPost() {
        return null;
    }

    @Override
    @Path("{path}")
    public Resource traverse(@PathParam("path") String path) {
        return super.traverse(path);
    }

    /**
     * Updates the current modified web page.
     */
    @POST
    @Path("modifyWebPage")
    public Object modifyWebPage() {
        log.debug("Modifying web page ...");
        try {
            CoreSession session = ctx.getCoreSession();
            HttpServletRequest request = ctx.getRequest();
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            Boolean isRichtext = SiteUtils.getBoolean(doc, WEBPAGE_EDITOR,
                    false);
            String content;
            if (isRichtext) {
                content = request.getParameter("richtextEditorEdit");
            } else {
                content = request.getParameter("wikitextEditorEdit");
            }
            String pushToMenu = request.getParameter("pushToMenu");

            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("dc:description", description);
            doc.setPropertyValue(WEBPAGE_CONTENT, content);
            doc.setPropertyValue(WEBPAGE_PUSHTOMENU,
                    Boolean.valueOf(pushToMenu));
            ContextTransmitterHelper.feedContext(doc);
            session.saveDocument(doc);
            session.save();
            DocumentModel webContainer = SiteUtils.getFirstWebSiteParent(
                    session, doc);
            String path = SiteUtils.getPagePath(webContainer, doc);
            return redirect(path);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }


    @Override
    protected String getSchemaFieldThemeName() {
        return WEBPAGE_SCHEMA_THEME;
    }

    @Override
    protected String getDefaultSchemaFieldThemeValue() {
        return DEFAULT_WEBPAGE_THEME_VALUE;
    }

    @Override
    protected String getSchemaFieldThemePageName() {
        return WEBPAGE_THEMEPAGE;
    }

    @Override
    protected String getDefaultSchemaFieldThemePageValue() {
        return DEFAULT_WEBPAGE_THEMEPAGE_VALUE;
    }

    @Override
    protected Map<String, Object> getErrorArguments() {
        Map<String, Object> errorArguments = new HashMap<String, Object>();
        errorArguments.put("pageName", pathSegment);
        return errorArguments;
    }

    @Override
    protected String getErrorTemplateName() {
        return "error_create_page.ftl";
    }

    @Override
    protected String getSearchThemePage() {
        return SEARCH_THEME_PAGE;
    }

    @Override
    protected String getDocumentDeletedErrorTemplateName() {
        return "error_deleted_page.ftl";
    }

}
