/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Value expression representing a map of value expressions, to allow resolution, at runtime, of all of the map values.
 * <p>
 * Useful to pass resolved widget properties to third party code (typically javascript code)
 *
 * @since 5.8
 */
public class MapValueExpression extends ValueExpression {

    private static final long serialVersionUID = 1L;

    protected final Map<String, ValueExpression> map;

    public MapValueExpression(Map<String, ValueExpression> map) {
        super();
        this.map = map == null ? Collections.<String, ValueExpression> emptyMap() : map;
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
    public Object getValue(ELContext elContext) throws PropertyNotFoundException {
        Map<String, Serializable> res = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, ValueExpression> entry : map.entrySet()) {
                ValueExpression ve = entry.getValue();
                if (ve != null) {
                    res.put(entry.getKey(), (Serializable) ve.getValue(elContext));
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
    public void setValue(ELContext arg0, Object arg1) throws PropertyNotFoundException {
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
