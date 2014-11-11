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

package org.nuxeo.ecm.platform.webengine.jsf.wiki;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.FreemarkerMacro;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.wikimodel.wem.WikiParserException;

@Name("wikiPreviewkActions")
@Scope(CONVERSATION)
public class WikiPreviewBean {

    @In(create = true)
    private NavigationContext navigationContext;

    protected FreemarkerEngine engine;

    private boolean initDone = false;

    public String getSimpleWikiPagePreview() throws IOException,
            WikiParserException {
        DocumentModel doc = navigationContext.getCurrentDocument();

        if (doc == null || !"WikiPage".equals(doc.getType())) {
            return "";
        } else {
            return getSimplePreview(doc);
        }
    }

    public String getWikiPagePreview() throws RenderingException {
        DocumentModel doc = navigationContext.getCurrentDocument();

        if (doc == null || !"WikiPage".equals(doc.getType())) {
            return "";
        } else {
            return getRenderPreview(doc);
        }
    }

    protected void initEngine() {
        if (initDone) {
            return;
        }

        engine = new FreemarkerEngine();
        WikiTransformer tr = new WikiTransformer();
        tr.getSerializer().addFilter(new PatternFilter(
                "[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>"));
        tr.getSerializer().addFilter(new PatternFilter("NXP-[0-9]+",
                "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));
        tr.getSerializer().registerMacro(new FreemarkerMacro());
        engine.setSharedVariable("wiki", tr);

        ResourceLocator locator = new StaticLocator();
        engine.setResourceLocator(locator);

        initDone = true;
    }

    protected String getRenderPreview(DocumentModel wikiPage)
            throws RenderingException {
        initEngine();

        engine.setSharedVariable("doc", wikiPage);

        StringWriter writer = new StringWriter();
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("doc", wikiPage);

        System.err.flush();
        double s = System.currentTimeMillis();
        engine.render("wiki_preview.ftl", input, writer);

        return writer.toString();
    }

    protected String getSimplePreview(DocumentModel wikiPage)
            throws IOException, WikiParserException {
        String wikiContent = (String) wikiPage.getProperty("wikiPage", "content");

        if (wikiContent == null) {
            return null;
        }

        Reader reader = new StringReader(wikiContent);

        WikiSerializer engine = new WikiSerializer();
        engine.addFilter(new PatternFilter("_([-A-Za-z0-9]+)_", "<i>$1</i>"));
        engine.addFilter(new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*",
                "<link>$0</link>"));
        engine.addFilter(new PatternFilter("NXP-[0-9]+",
                "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>"));

        StringWriter writer = new StringWriter();
        engine.serialize(reader, writer);

        return writer.toString();
    }

}
