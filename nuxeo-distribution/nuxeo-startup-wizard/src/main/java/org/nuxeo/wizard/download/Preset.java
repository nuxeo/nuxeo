/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.download;

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class Preset {

    protected final String id;

    protected final String label;

    protected final String[] pkgs;

    public Preset(String id, String label, String[] pkgs) {
        this.id = id;
        this.label = label;
        this.pkgs = pkgs;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String[] getPkgs() {
        return pkgs;
    }

    public String getPkgsAsJsonArray() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < pkgs.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("'" + pkgs[i] + "'");
        }
        sb.append("]");
        return sb.toString();
    }

}
