/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.el.ELContext;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

/**
 * Value expression representing a map of value expressions, to allow
 * resolution, at runtime, of all of the map values.
 * <p>
 * Useful to pass resolved widget properties to third party code (typically
 * javascript code)
 *
 * @since 5.8
 */
public class MapValueExpression extends ValueExpression {

    private static final long serialVersionUID = 1L;

    protected final Map<String, ValueExpression> map;

    public MapValueExpression(Map<String, ValueExpression> map) {
        super();
        this.map = map == null ? Collections.<String, ValueExpression> emptyMap()
                : map;
    }

    @Override
    public Class<?> getExpectedType() {
        return Map.class;
    }

    @Override
    public Class<?> getType(ELContext arg0) throws PropertyNotFoundException {
        return Map.class;
    }

    @Override
    public Object getValue(ELContext elContext)
            throws PropertyNotFoundException {
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        if (map != null) {
            for (Map.Entry<String, ValueExpression> entry : map.entrySet()) {
                ValueExpression ve = entry.getValue();
                if (ve != null) {
                    res.put(entry.getKey(),
                            (Serializable) ve.getValue(elContext));
                } else {
                    res.put(entry.getKey(), null);
                }
            }
        }
        return res;
    }

    @Override
    public boolean isReadOnly(ELContext arg0) throws PropertyNotFoundException {
        return true;
    }

    @Override
    public void setValue(ELContext arg0, Object arg1)
            throws PropertyNotFoundException {
        // do nothing
    }

    @Override
    public String getExpressionString() {
        return null;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MapValueExpression)) {
            return false;
        }
        MapValueExpression other = (MapValueExpression) obj;
        return map.equals(other.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

}
