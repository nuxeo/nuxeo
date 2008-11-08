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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree;

public class FakeContentProvider implements ContentProvider {

    private static final long serialVersionUID = -5447072937714133528L;

    public Object[] getChildren(Object obj) {
        return null;
    }

    public Object[] getElements(Object input) {
        return null;
    }

    public String[] getFacets(Object object) {
        return null;
    }

    public String getLabel(Object obj) {
        return null;
    }

    public String getName(Object obj) {
        return null;
    }

    public boolean isContainer(Object obj) {
        return false;
    }

}
