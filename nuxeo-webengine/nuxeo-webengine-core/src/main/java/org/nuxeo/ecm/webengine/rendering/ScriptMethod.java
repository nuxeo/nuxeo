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

package org.nuxeo.ecm.webengine.rendering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptMethod implements TemplateMethodModelEx {

    public Object exec(List arguments) throws TemplateModelException {
        int size = arguments.size();
        if (size < 1) {
            throw new TemplateModelException("Invalid number of arguments for script(...) method");
        }

        SimpleScalar val = (SimpleScalar)arguments.get(0);
        if (val == null) {
            throw new TemplateModelException("src attribute is required");
        }
        String src = val.getAsString();

        Map<String,Object> args = new HashMap<String, Object>();
        if (arguments.size() > 1) {
            Object o = arguments.get(1);
            if (o instanceof SimpleScalar) {
                String arg = ((SimpleScalar) o).getAsString();
                args.put("_args", new String[] {arg});
            } else if (!(o instanceof TemplateHashModelEx)) {
                throw new TemplateModelException("second argument should be a map");
            } else {
                TemplateHashModelEx t = (TemplateHashModelEx) o;
                TemplateCollectionModel keys = t.keys();
                TemplateModelIterator it = keys.iterator();

                while (it.hasNext()) {
                    TemplateModel k = it.next();
                    String kk = k.toString();
                    TemplateModel v = t.get(kk);
                    Object vv = null;
                    if (v instanceof AdapterTemplateModel) {
                        vv = ((AdapterTemplateModel) v).getAdaptedObject(null);
                    } else {
                        vv = v.toString();
                    }
                    args.put(kk, vv);
                }
            }
        }

        WebContext ctx = WebEngine.getActiveContext();
        if (ctx != null) {
            try {
                return ctx.runScript(src, args);
            } catch (WebException e) {
                throw new TemplateModelException("Failed to run script: "+src, e);
            }
        } else {
            throw new IllegalStateException("Not In a Web Context");
        }
    }

}
