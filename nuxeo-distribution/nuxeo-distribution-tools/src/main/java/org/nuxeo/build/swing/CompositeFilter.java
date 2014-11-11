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
 */
package org.nuxeo.build.swing;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeFilter implements Filter {

    protected Filter preset;
    protected Filter prefix;

    public void setPrefixFilter(Filter prefix) {
        this.prefix = prefix;
    }

    public void setPresetFilter(Filter preset) {
        this.preset = preset;
    }

    public void removePrefixFilter() {
        this.prefix = null;
    }

    public void removePresetFilter() {
        this.preset = null;
    }


    public Filter getPrefixFilter() {
        return prefix;
    }

    public Filter getPresetFilter() {
        return preset;
    }


    public boolean acceptRow(String key) {
        if (preset != null && !preset.acceptRow(key)) {
            return false;
        }
        if (prefix != null && !prefix.acceptRow(key)) {
            return false;
        }
        return true;
    }

}
