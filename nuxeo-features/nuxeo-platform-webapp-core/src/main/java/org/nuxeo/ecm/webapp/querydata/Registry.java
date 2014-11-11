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

/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: Registry.java 3034 2006-09-18 16:54:54Z janguenot $
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic registry implementation.
 *
 * @author <a href="mailto:akalogeropoulos@nuxeo.com">Andreas Kalogeropoulos</a>
 */
@Deprecated
public class Registry implements Serializable {

    private static final long serialVersionUID = -1063443973976908727L;

    private static final Log log = LogFactory.getLog(Registry.class);

    public final String name;

    private final Map<String, Object> registry;

    public Registry(String name) {
        this.name = name;
        registry = new HashMap<String, Object>();
    }

    public String getName() {
        return name;
    }

    public void register(String name, Object object) {
        registry.put(name, object);
    }

    public void unregister(String name) {
        if (isRegistred(name)) {
            registry.remove(name);
        }
    }

    public boolean isRegistred(Object object) {
        return registry.containsValue(object);
    }

    public boolean isRegistred(String name) {
        return registry.containsKey(name);
    }

    public int size() {
        return registry.size();
    }

    public Object getObjectByName(String name) {
        return registry.get(name);
    }

    public void clear() {
        registry.clear();
    }

}
