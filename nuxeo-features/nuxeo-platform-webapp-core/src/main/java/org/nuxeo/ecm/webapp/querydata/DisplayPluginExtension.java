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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.querydata;

import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.Element;


@Deprecated
@XObject("displayplugin")
public class DisplayPluginExtension implements DisplayPlugin {

    private static final long serialVersionUID = -7995778067970081651L;

    @XNode("@name")
    String name;

    @XNode("@valid")
    String valid;

    @XNode("columns")
    Element columns = null;

    DisplayExtensionConf conf;


    public DisplayPluginExtension() {
    }

    public DisplayPluginExtension(String name, String valid) {
        this.name = name;
        this.valid = valid;
    }

    public void setColumnsChain() {
        conf = new DisplayExtensionConf();
        conf.setElement(columns);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Map<String, String>> getColumns() {
        return conf.getColumns();
    }

    public String getValid() {
        return valid;
    }

    public void setValid(String valid) {
        this.valid = valid;
    }

}
