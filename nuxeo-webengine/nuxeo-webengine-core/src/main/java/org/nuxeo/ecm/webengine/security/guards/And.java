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

package org.nuxeo.ecm.webengine.security.guards;

import java.util.Collection;

import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class And implements Guard {

    protected Guard[] perms;

    public And(Collection<Guard> guards) {
        this(guards.toArray(new Guard[guards.size()]));
    }

    public And(Guard... perms) {
        if (perms == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
        this.perms = perms;
    }

    @Override
    public boolean check(Adaptable context) {
        for (Guard perm : perms) {
            if (!perm.check(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (perms == null) {
            return "[AND]";
        }
        buf.append('(').append(perms[0]);
        for (int i = 1; i < perms.length; i++) {
            buf.append(" AND ").append(perms[i]);
        }
        return buf.append(')').toString();
    }

}
