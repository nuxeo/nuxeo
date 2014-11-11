/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.wiki.rendering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;


public class WikiPageLinkResolver implements WikiFilter{

    private static final String LINK_CLASS_EXIST = "exist";
    private static final String LINK_CLASS_DONTEXISTS = "dontexists";
    // TODO get this from config files
    public static final String PATTERN = "(\\.)?([A-Z]+[a-z]+[A-Z][A-Za-z]*\\.)*([A-Z]+[a-z]+[A-Z][A-Za-z]*)";
    public static final Pattern PAGE_LINK_PATTERN = Pattern.compile(PATTERN);

    static final String LINK_TEMPLATE = "<a  href=\"%s\" class=\"%s\">%s</a>";

    public String apply(String content) {
        Matcher m = PAGE_LINK_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        if (!m.find()) {
            return content;
        }
        do {
            String s = m.group();
            String link = builsLinks(s);
            m.appendReplacement(sb, link);
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    protected String builsLinks(String pageName) {
        DocumentModel doc;
        String basePath;

        WebContext ctx = WebEngine.getActiveContext();
        Resource resource = ctx.getTargetObject();
        StringBuffer links = new StringBuffer();
        StringBuffer relativePath  = new StringBuffer();

        /*
         * WEB-228 Remove the if branch for the moment.
         * This will be reconsidered by WEB-236
         *

        if (pageName.startsWith(".")) { // Absolute path
            basePath = ctx.getModulePath();
            String[] segments = pageName.substring(1).split("\\.");
            doc = getWikisRoot();
            for (String s : segments) {
                links.append(".");
                relativePath.append("/").append(s);
                if (doc != null) {
                    doc = getDocument(doc, s);
                }
                links.append(buildLink(basePath, relativePath, s, doc));
            }
        } else { // relative path
         */
            basePath = resource.getPrevious().getPath();
            doc = getResourceDocument(resource.getPrevious());
            String[] segments = pageName.split("\\.");
            for (String s : segments) {
                relativePath.append("/").append(s);
                if (doc != null) {
                    doc = getDocument(doc, s);
                }
                links.append(buildLink(basePath, relativePath, s, doc));
                links.append(".");
        /*
         * WEB-228 Remove the if branch for the moment.
         * This will be reconsidered by WEB-236
         *
            }
        */
            // remove last dot
            links.deleteCharAt(links.length() - 1);
        }
        return links.toString();
    }

    protected String buildLink(String basePath, StringBuffer relativePath, String s, DocumentModel doc){
        String linkClass;
        if ( doc == null ){
            linkClass = LINK_CLASS_DONTEXISTS;
        } else {
            linkClass = LINK_CLASS_EXIST;
        }
        return String.format(LINK_TEMPLATE, basePath + relativePath  , linkClass, s);
    }

    /*
     * WEB-228 Remove getWikisRoot() method for the moment.
     *
    protected DocumentModel getWikisRoot(){
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();
        DocumentRef ref = new PathRef("/default-domain/workspaces/wikis");
        try {
            return session.getDocument(ref);
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    protected DocumentModel getResourceDocument(Resource resource) {
        if (resource instanceof DocumentObject) {
            DocumentObject docObj = (DocumentObject) resource;
            return docObj.getDocument();
        }
        return null;
    }

    private static DocumentModel getDocument(DocumentModel doc, String segment) {
        WebContext ctx = WebEngine.getActiveContext();
        CoreSession session = ctx.getCoreSession();
        Path p = doc.getPath().append(segment);
        DocumentRef ref = new PathRef(p.toString());
        try {
            if (session.exists(ref)) {
                return session.getDocument(ref);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

}
