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

package org.nuxeo.ecm.webengine.mapping;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.PathInfo;
import org.nuxeo.ecm.webengine.util.Attributes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("mapping")
public class MappingDescriptor {

    PathPattern pattern;
    VariableString action;
    VariableString script;
    VariableString document;

    @XNode("@pattern")
    public void setPattern(String pattern) {
        this.pattern = new PathPattern(pattern);
    }

    @XNode("document")
    public void setRoot(RootMapping rootMapping) {
        if (rootMapping != null) {
            this.document = rootMapping.root;
        }
    }

    @XNode("action")
    public void setAction(String action) {
        if (action != null) {
            this.action = VariableString.parse(action);
        }
    }


    @XNode("script")
    public void setScript(String script) {
        if (script != null) {
            this.script = VariableString.parse(script);
        }
    }

    public final boolean rewrite(String input, PathInfo pathInfo) {
        Attributes attrs = pattern.match(input);
        if (attrs == null) {
            return false;
        }
        // it's matching - do the rewrite
        pathInfo.setAttributes(attrs);
        if (document != null) {
            String val = this.document.getValue(attrs);
            if (val.startsWith("/")) {
                pathInfo.setDocument(new PathRef(val));
            } else {
                pathInfo.setDocument(new IdRef(val));
            }
        }
        if (script != null) {
            pathInfo.setScript(this.script.getValue(attrs));
        }
        if (action != null) {
            pathInfo.setAction(this.action.getValue(attrs));
        }
        return true;
    }


}
