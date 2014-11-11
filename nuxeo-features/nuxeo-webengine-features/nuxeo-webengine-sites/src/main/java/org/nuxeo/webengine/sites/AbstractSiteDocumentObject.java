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

import static org.nuxeo.webengine.sites.utils.SiteConstants.EMAIL;
import static org.nuxeo.webengine.sites.utils.SiteConstants.PAGE_NAME;
import static org.nuxeo.webengine.sites.utils.SiteConstants.PAGE_NAME_ATTRIBUTE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.PERMISSION_ADD_CHILDREN;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_PARAM;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_PARAM_DOC_TYPE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SITE_DESCRIPTION;
import static org.nuxeo.webengine.sites.utils.SiteConstants.THEME_BUNDLE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.THEME_PERSPECTIVE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.VIEW_PERSPECTIVE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_BASELINE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_EMAIL;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_NAME;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_URL;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_WELCOMEMEDIA;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * The basic web object implementation. It holds the web object back methods.
 *
 * @author rux
 */
@WebObject(type = "AbstractSiteDocumentObject", superType = "Document", facets = { "AbstractSiteDocumentObject" })
@Produces("text/html;charset=UTF-8")
public abstract class AbstractSiteDocumentObject extends DocumentObject {

    private static final Log log = LogFactory.getLog(AbstractSiteDocumentObject.class);

    public static final int NO_WELCOME_MEDIA = -1;

    public static final int IMAGE_WELCOME_MEDIA = 0;

    public static final int FLASH_WELCOME_MEDIA = 1;

    private boolean forceRedirectToLogout = false;

    /**
     * Executes the GET requests on the current web object
     */
    @GET
    @Override
    public Object doGet() {
        if (doc == null && !forceRedirectToLogout) {
            return getTemplate(getErrorTemplateName()).args(getErrorArguments());
        }
        if (doc == null && forceRedirectToLogout) {
            return handleAnonymousRedirectToLogout(ctx.getRequest());
        }
        try {
            if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                return getTemplate(getDocumentDeletedErrorTemplateName()).args(
                        getErrorArguments());
            }
        } catch (ClientException e1) {
            throw WebException.wrap(e1);
        }
        setDoGetParameters();
        try {
            return getTemplate("template_default.ftl").args(getArguments());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Retrieves the logo of the current web object.
     */
    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            DocumentModel parentWebSite = getParentWebSite(ctx.getCoreSession());
            resp = SiteUtils.getLogoResponse(parentWebSite);
        } catch (Exception e) {
            if (doc == null) {
                log.error("Problems retrieving the logo", e);
            } else {
                log.error("Problems retrieving the logo for" + doc.getName(), e);
            }
        }
        // return a default image, maybe you want to change this in future
        if (resp == null) {
            resp = redirect(getContext().getModule().getSkinPathPrefix()
                    + "/images/logo.gif");
        }
        return resp;
    }

    /**
     * Returns the welcome media of the current web object.
     */
    @GET
    @Path("welcomeMedia")
    public Response getWelcomeMedia() {
        Response resp = null;
        try {
            DocumentModel parentWebSite = getParentWebSite(ctx.getCoreSession());
            Blob blob = SiteUtils.getBlob(parentWebSite,
                    WEBCONTAINER_WELCOMEMEDIA);
            if (blob != null) {
                resp = Response.ok().entity(blob).type(blob.getMimeType()).build();
            }
        } catch (Exception e) {
            log.error("Error while trying to display the website. ", e);
        }
        // return a default image, maybe you want to change this in future
        if (resp == null) {
            resp = redirect(getContext().getModule().getSkinPathPrefix()
                    + "/images/logo.gif");
        }
        return resp;
    }

    public int getWelcomeMediaWidth() {
        return 200;
    }

    public int getWelcomeMediaHeight() {
        return 100;
    }

    public int getWelcomeMediaIsImage() throws Exception {
        DocumentModel parentWebSite = getParentWebSite(ctx.getCoreSession());
        Blob blob = SiteUtils.getBlob(parentWebSite, WEBCONTAINER_WELCOMEMEDIA);
        if (blob == null || blob.getMimeType() == null) {
            return NO_WELCOME_MEDIA;
        } else if (blob.getMimeType().startsWith("image/")) {
            return IMAGE_WELCOME_MEDIA;
        } else {
            return FLASH_WELCOME_MEDIA;
        }
    }

    /**
     * Method called before a search operation is made for the context of the
     * current web object.
     */
    @GET
    @Path("search")
    public Object getSearchParameters() {
        setSearchParameters();
        try {
            return getTemplate("template_default.ftl").args(getArguments());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * JAX-RS specs doesn't allow multiple REST designator on a single method so
     * we need to use another method to do a POST.
     */
    @POST
    @Path("search")
    public Object _getSearchParameters() {
        return getSearchParameters();
    }

    @Override
    @Path("{path}")
    public Resource traverse(@PathParam("path") String path) {
        try {
            return newDocument(path);
        } catch (Exception e) {
            if (e instanceof WebResourceNotFoundException) {
                CoreSession session = ctx.getCoreSession();
                try {
                    if (session.hasPermission(doc.getRef(),
                            PERMISSION_ADD_CHILDREN)) {
                        DocumentObject parent = (DocumentObject) ctx.getTargetObject();
                        ctx.getRequest().setAttribute(THEME_PERSPECTIVE,
                                "create");
                        ctx.getRequest().setAttribute(PAGE_NAME_ATTRIBUTE, path);
                        return parent;
                    } else {
                        return newObject(getWebPageDocumentType(), path);
                    }
                } catch (ClientException ce) {
                    throw WebException.wrap(ce);
                }
            } else {
                throw WebException.wrap(e);
            }
        }
    }

    /**
     * Creates a new web page object.
     */
    @POST
    @Path("createWebPage")
    public Object createWebPage() {
        try {
            CoreSession session = ctx.getCoreSession();
            DocumentModel createdDocument = SiteUtils.createDocument(
                    ctx.getRequest(), session, doc.getPathAsString(),
                    getWebPageDocumentType());
            DocumentModel parentWebSite = getParentWebSite(session);
            String path = SiteUtils.getPagePath(parentWebSite, createdDocument);
            return redirect(path.toString());
            //return redirect(URIUtils.quoteURIPathComponent(path, false));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Method used to retrieve the path to the current web container.
     *
     * @return the path to the current web container
     */
    @GET
    @Path("homePagePath")
    public Object getHomePagePath() {
        try {
            DocumentModel parentWebSite = getParentWebSite(ctx.getCoreSession());
            StringBuilder path = new StringBuilder(
                    SiteUtils.getWebContainersPath()).append("/");
            path.append(SiteUtils.getString(parentWebSite, WEBCONTAINER_URL));
            return redirect(path.toString());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Method used to delete pages in the current container
     *
     * @return the path to the current web container
     */

    @GET
    @Path("delete")
    public Response remove() {
        return doDelete();
    }

    @DELETE
    public Response doDelete() {
        CoreSession session = ctx.getCoreSession();
        try {
            DocumentModel webContainer = SiteUtils.getFirstWebSiteParent(
                    session, doc);
            DocumentRef docRef = doc.getRef();
            if (session.getAllowedStateTransitions(docRef).contains(
                    LifeCycleConstants.DELETE_TRANSITION)) {
                session.followTransition(docRef,
                        LifeCycleConstants.DELETE_TRANSITION);
            } else {
                session.removeDocument(docRef);
            }
            session.save();

            return redirect( SiteUtils.getPagePath(webContainer, webContainer));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Returns the document type of the container.
     */
    protected String getWebSiteDocumentType() {
        return WEBSITE;
    }

    /**
     * Returns the document type of the entry.
     */
    protected String getWebPageDocumentType() {
        return WEBPAGE;
    }

    /**
     * Sets the parameters needed to perform a search.
     */
    protected void setSearchParameters() {
        ctx.getRequest().setAttribute(THEME_BUNDLE, getSearchThemePage());
        ctx.setProperty(SEARCH_PARAM, ctx.getRequest().getParameter(
                "searchParam"));
        ctx.setProperty(SEARCH_PARAM_DOC_TYPE, getWebPageDocumentType());
    }

    /**
     * Returns the map with the arguments that will be used to generate the
     * default template page for the current web object.
     */
    protected Map<String, Object> getArguments() throws Exception {

        Map<String, Object> root = new HashMap<String, Object>();
        DocumentModel parentWebSite = getParentWebSite(getCoreSession());

        root.put(PAGE_NAME, SiteUtils.getString(parentWebSite,
                WEBCONTAINER_NAME, ""));
        root.put(SITE_DESCRIPTION, SiteUtils.getString(parentWebSite,
                WEBCONTAINER_BASELINE, ""));
        root.put(EMAIL, "mailto:"
                + SiteUtils.getString(parentWebSite, WEBCONTAINER_EMAIL, ""));
        // specific only for Page web object
        MimetypeRegistry mimetypeService = Framework.getService(MimetypeRegistry.class);
        root.put("mimetypeService", mimetypeService);
        return root;
    }

    /**
     * Sets the parameters that will be needed in order to execute the GET
     * requests on the current web object.
     */
    protected void setDoGetParameters() {
        // getting theme config from document.
        String theme = SiteUtils.getString(doc, getSchemaFieldThemeName(),
                getDefaultSchemaFieldThemeValue());
        String themePage = SiteUtils.getString(doc,
                getSchemaFieldThemePageName(),
                getDefaultSchemaFieldThemePageValue());

        ctx.getRequest().setAttribute(THEME_BUNDLE, theme + "/" + themePage);

        String currentPerspective = (String) ctx.getRequest().getAttribute(
                THEME_PERSPECTIVE);
        if (StringUtils.isEmpty(currentPerspective)) {
            // Set view perspective if none present.
            ctx.getRequest().setAttribute(THEME_PERSPECTIVE, VIEW_PERSPECTIVE);
        }
    }

    /**
     * Returns the parent web site of the current web object.
     *
     * @param session the nuxeo core session
     * @return the parent web site of the current web object
     */
    protected DocumentModel getParentWebSite(CoreSession session)
            throws Exception {
        DocumentModel parentWebSite = null;
        if (!doc.hasFacet(SiteConstants.WEB_VIEW_FACET)) {
            parentWebSite = SiteUtils.getFirstWebSiteParent(session, doc);
        } else {
            parentWebSite = doc;
        }
        return parentWebSite;
    }

    /**
     * Returns the search theme page.
     */
    protected abstract String getSearchThemePage();

    /**
     * Returns the schema name plus field name which together keep the theme for
     * the current web object.
     */
    protected abstract String getSchemaFieldThemeName();

    /**
     * Returns the schema name plus field name which together keep the theme
     * page for the current web object.
     */
    protected abstract String getSchemaFieldThemePageName();

    /**
     * Returns the default value of the theme for the current web object.
     */
    protected abstract String getDefaultSchemaFieldThemeValue();

    /**
     * Returns the default value of the theme page for the current web object.
     */
    protected abstract String getDefaultSchemaFieldThemePageValue();

    /**
     * Returns the name of the template that will be used in case the
     * DocumentModel for the current web object is null.
     */
    protected abstract String getErrorTemplateName();

    /**
     * Returns the name of the template that will be used in case the
     * DocumentModel is in "deleted" state
     */
    protected abstract String getDocumentDeletedErrorTemplateName();

    /**
     * Returns the map with the arguments that will be used to generate the
     * error template page.
     */
    protected abstract Map<String, Object> getErrorArguments();

    protected Response handleAnonymousRedirectToLogout(
            HttpServletRequest request) {
        Map<String, String> urlParameters = new HashMap<String, String>();
        urlParameters.put(NXAuthConstants.SECURITY_ERROR, "true");
        urlParameters.put(NXAuthConstants.FORCE_ANONYMOUS_LOGIN, "true");
        if (request.getAttribute(NXAuthConstants.REQUESTED_URL) != null) {
            urlParameters.put(
                    NXAuthConstants.REQUESTED_URL,
                    (String) request.getAttribute(NXAuthConstants.REQUESTED_URL));
        } else {
            urlParameters.put(NXAuthConstants.REQUESTED_URL,
                    NuxeoAuthenticationFilter.getRequestedUrl(request));
        }
        String baseURL = "";
        try {
            baseURL = initAuthenticationService().getBaseURL(request)
                    + NXAuthConstants.LOGOUT_PAGE;
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
        request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
        baseURL = URIUtils.addParametersToURIQuery(baseURL, urlParameters);
        setForceRedirectToLogout(false);
        return redirect(baseURL);
    }

    protected void setForceRedirectToLogout(boolean redirectToLogout) {
        this.forceRedirectToLogout = redirectToLogout;
    }

    protected PluggableAuthenticationService initAuthenticationService()
            throws ClientException {
        PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            log.error("Unable to get Service "
                    + PluggableAuthenticationService.NAME);
            throw new ClientException(
                    "Can't initialize Nuxeo Pluggable Authentication Service");
        }
        return service;
    }
}