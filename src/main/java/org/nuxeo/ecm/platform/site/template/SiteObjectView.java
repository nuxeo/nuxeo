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

import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("action")
public class SiteObjectView {

    public final static SiteObjectView DISABLED = new SiteObjectView();

    @XNode("@name")
    public String name;

    public URL template;

    @XNode("@enabled")
    public boolean isEnabled = true;

    public SiteObjectView() {

    }

    public SiteObjectView(String name, URL template) {
        this.name = name;
        this.template = template;
    }

    public void setTemplate(URL url) {
        this.template = url;
    }

    public URL getTemplate() {
        return template;
    }

    @XNode("@template")
    void setTemplate(String url) throws MalformedURLException {
        this.template = new URL(url);
    }

}
