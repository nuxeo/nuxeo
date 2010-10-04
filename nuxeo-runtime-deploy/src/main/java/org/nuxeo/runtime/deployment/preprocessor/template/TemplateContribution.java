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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.template;

import org.nuxeo.common.xmap.Resource;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject
public class TemplateContribution {

    @XNode("@src")
    private Resource src;

    private String template;

    private String marker;

    @XContent
    private String content;

    @XNode("@mode")
    private String mode = "append";


    @XNode("@target")
    public void setTarget(String target) {
        int p = target.lastIndexOf('#');
        if (p > -1) {
            template = target.substring(0, p);
            marker = target.substring(p + 1);
        } else {
            template = target;
            marker = Template.END;
        }
    }

    public String getContent() {
        if (content == null) {
            if (src == null) {
                return "";
            }
            return src.toString();
        }
        return content;
    }

    public String getTemplate() {
        return template;
    }

    public String getMarker() {
        return marker;
    }

    public String getMode() {
        return mode;
    }

    public boolean isAppending() {
        return "append".equals(mode);
    }

    public boolean isPrepending() {
        return "prepend".equals(mode);
    }

    public boolean isReplacing() {
        return "replace".equals(mode);
    }

}
