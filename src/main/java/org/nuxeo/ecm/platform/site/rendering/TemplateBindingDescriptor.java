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

package org.nuxeo.ecm.platform.site.rendering;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("templateBinding")
public class TemplateBindingDescriptor {

    @XNode("@templateName")
    public String templateName;

    @XNode("@path")
    public String path = "*";


    @XNode("@docType")
    public String docType;


    public String getTemplateName() {
        return templateName;
    }


    @Override
    public String toString() {
        return templateName + "(" + docType + ":" + path + ")";
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getPath() {
        if (path == null || "".equals(path)) {
            return "*";
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

}
