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

package org.nuxeo.ecm.platform.site.template;

import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScriptEngine {

    RenderingEngine renderingEngine;

    public ScriptEngine(RenderingEngine engine) {
        renderingEngine = engine;
    }

    public void exec(SiteRequest req) throws Exception {

        ScriptFile script = req.getScript();
        String ext = script.getExtension();
        if ("ftl".equals(ext)) {
            renderingEngine.render(req.getScript().getPath(), req.getLastResolvedObject());
        } else {
            throw new SiteException("Scripts are not yet supported");
        }

    }

}
