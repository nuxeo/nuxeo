/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 *     Florent Guillaume
 */

package org.nuxeo.webengine.sites;

import static org.nuxeo.webengine.sites.utils.SiteConstants.TAG_DOCUMENT;
import static org.nuxeo.webengine.sites.utils.SiteConstants.THEME_BUNDLE;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Adapter used to display all documents for a certain tag.
 */
@WebAdapter(name = "tag", type = "TagAdapter", targetType = "Document")
@Produces("text/html;charset=UTF-8")
public class TagAdapter extends DefaultAdapter {

    @GET
    @Path("{id}")
    public Object changePerspective(@PathParam("id") String tagId) {
        try {
            DocumentObject documentObject = (DocumentObject) getTarget();

            ctx.setProperty(TAG_DOCUMENT, tagId);
            if (documentObject instanceof AbstractSiteDocumentObject) {
                AbstractSiteDocumentObject abstractSiteDocumentObject = (AbstractSiteDocumentObject) documentObject;
                ctx.getRequest().setAttribute(THEME_BUNDLE,
                        abstractSiteDocumentObject.getSearchThemePage());
                return getTemplate("template_default.ftl").args(
                        abstractSiteDocumentObject.getArguments());
            }
            return getTemplate("template_default.ftl");
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("addTagging")
    public Object addTagging() {
        try {
            DocumentObject documentObject = (DocumentObject) getTarget();
            DocumentModel doc = documentObject.getDocument();
            CoreSession session = ctx.getCoreSession();
            String tagLabel = ctx.getRequest().getParameter("tagLabel");
            TagService tagService = Framework.getService(TagService.class);
            if (tagService != null) {
                // Insert multiple tags if separated by commas
                String[] tagLabelArray = tagLabel.split(",");
                for (String label : tagLabelArray) {
                    label = label.trim();
                    if (label.length() > 0) {
                        tagService.tag(session, doc.getId(), label, null);
                    }
                }
            }

            String path = SiteUtils.getPagePath(
                    SiteUtils.getFirstWebSiteParent(session, doc), doc);
            return redirect(path);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("removeTagging")
    public Object removeTagging() {

        try {
            DocumentObject documentObject = (DocumentObject) getTarget();
            DocumentModel doc = documentObject.getDocument();
            CoreSession session = ctx.getCoreSession();
            String label = ctx.getRequest().getParameter("taggingId");
            TagService tagService = Framework.getService(TagService.class);
            if (tagService != null) {
                tagService.untag(session, doc.getId(), label, null);
            }

            String path = SiteUtils.getPagePath(
                    SiteUtils.getFirstWebSiteParent(session, doc), doc);
            return redirect(path);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
