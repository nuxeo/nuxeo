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

package org.nuxeo.runtime.contribution;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyObject {

    public String id;
    public String fid;
    public String str;
    public List<String> list;
    public String[] parents;

    public MyObject() {
    }

    public MyObject(String id, String fid) {
        this.id = id;
        this.fid = fid;
    }

    public Object[] getExtends() {
        return parents;
    }

    @Override
    public String toString() {
        return id + ":" + fid + " = str: '" + str + "', list: [ " + list + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MyObject) {
            MyObject myf = (MyObject)obj;
            if (!id.equals(myf.id)) {
                return false;
            }
            if (fid == null || myf.fid == null) {
                return fid == myf.fid;
            }
            return fid.equals(myf.fid);
        }
        return false;
    }

}
