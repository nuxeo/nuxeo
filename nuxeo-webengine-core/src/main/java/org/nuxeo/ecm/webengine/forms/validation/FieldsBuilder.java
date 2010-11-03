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
package org.nuxeo.ecm.webengine.forms.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FieldsBuilder {

    protected Map<String, String[]> map;

    public static FieldsBuilder create() {
        return new FieldsBuilder();
    }

    public static FieldsBuilder create(Map<String, String[]> map) {
        return new FieldsBuilder(map);
    }

    public FieldsBuilder() {
        map = new HashMap<String, String[]>();
    }

    public FieldsBuilder(Map<String, String[]> map) {
        this.map = map;
    }

    public FieldsBuilder put(String key, String ... value) {
        map.put(key, value);
        return this;
    }

    public FieldsBuilder put(String key, Collection<String> value) {
        map.put(key, value.toArray(new String[value.size()]));
        return this;
    }

    public Map<String,String[]> fields() {
        return map;
    }
}
