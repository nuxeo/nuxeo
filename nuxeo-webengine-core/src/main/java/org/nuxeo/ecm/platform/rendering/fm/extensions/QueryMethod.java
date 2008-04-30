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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RenderingContextModel;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class QueryMethod implements TemplateMethodModelEx {

    protected SearchService search;
    protected boolean useCoreSearch = false;

    public Object exec(List arguments) throws TemplateModelException {
        if (arguments.size() != 1) {
            throw new TemplateModelException("Invalid number of arguments for query(nxql) method");
        }
        String query = null;
        SimpleScalar scalar = (SimpleScalar)arguments.get(0);
        if (scalar != null) {
            query = scalar.getAsString();
        } else {
            throw new TemplateModelException("the argument is not defined");
        }

        RenderingContextModel ctxModel = FreemarkerEngine.getContextModel();
        if (ctxModel == null) {
            throw new TemplateModelException("Not in a nuxeo rendering context");
        }
        CoreSession session = ctxModel.getContext().getSession();

        try {
            if (useCoreSearch) {
                return session.query(query);
            } else {
                if (search == null) {
                    search = Framework.getService(SearchService.class);
                    if (search == null) {
                        useCoreSearch = true;
                        return session.query(query);
                    }
                }
                ResultSet result = search.searchQuery(new ComposedNXQueryImpl(query), 0, Integer.MAX_VALUE);
                DocumentModelList docs = new DocumentModelListImpl();
                for (ResultItem item : result) {
                    String id = (String)item.get("ecm:uuid");
                    DocumentModel doc = session.getDocument(new IdRef(id));
                    docs.add(doc);
                }
                return docs;
            }
        } catch (Exception e) {
            throw new TemplateModelException("Failed to perform search: "+query, e);
        }
    }

}
