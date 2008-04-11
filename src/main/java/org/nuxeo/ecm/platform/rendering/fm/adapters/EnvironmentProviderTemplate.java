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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.ecm.platform.rendering.api.EnvironmentProvider;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleCollection;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EnvironmentProviderTemplate implements TemplateHashModelEx, AdapterTemplateModel {

    protected RootContextModel root; // the root context

    public EnvironmentProviderTemplate(RootContextModel ctx) {
        root = ctx;
    }

    @SuppressWarnings("unchecked")
    public Object getAdaptedObject(Class hint) {
        return root.getEngine().getEnvironmentProvider();
    }

    public EnvironmentProvider getEnvironmentProvider() {
        return root.getEngine().getEnvironmentProvider();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object o = root.getEngine().getEnvironmentProvider().getEnv(key, root.getThisContext());
        return root.getObjectWrapper().wrap(o);
    }

    /**
     * A doc model is never empty.
     */
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        EnvironmentProvider ep = root.getEngine().getEnvironmentProvider();
        Collection<String> keys =  ep.getKeys();
        return new SimpleCollection(keys);
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        EnvironmentProvider ep = root.getEngine().getEnvironmentProvider();
        RenderingContext ctx = root.getThisContext();
        Collection<String> keys =  ep.getKeys();
        Collection<Object> values = new ArrayList<Object>();
        for (String key : keys) {
            ep.getEnv(key, ctx);
        }
        return new SimpleCollection(values);
    }

    public int size() throws TemplateModelException {
        return root.getEngine().getEnvironmentProvider().size();
    }

}
