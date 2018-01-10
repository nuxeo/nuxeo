/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            MyObject myf = (MyObject) obj;
            if (!id.equals(myf.id)) {
                return false;
            }
            if (!fid.equals(myf.fid)) {
                return false;
            }
            return true;
        }
        return false;
    }

}
