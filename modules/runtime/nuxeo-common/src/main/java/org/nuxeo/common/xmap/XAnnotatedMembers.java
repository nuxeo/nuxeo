/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.common.xmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodes;
import org.w3c.dom.Element;

/**
 * Handles multiple annotations.
 *
 * @since 11.5
 */
public class XAnnotatedMembers<T> extends XAnnotatedMember<T> {

    protected XAnnotatedMember<T>[] members;

    protected String separator;

    public XAnnotatedMembers(XMap xmap, XAccessor<T> setter, XAnnotatedMember<T>[] members, String separator,
            String defaultValue) {
        super(xmap, setter);
        init(xmap, setter, members, separator, defaultValue);
    }

    public XAnnotatedMembers(XMap xmap, XAccessor<T> setter, XNodes anno) {
        this(xmap, setter, anno.values(), anno.separator(), anno.defaultAssignment());
    }

    public XAnnotatedMembers(XMap xmap, XAccessor<T> setter, String[] values, String separator, String defaultValue) {
        super(xmap, setter);
        XAnnotatedMember<T>[] members = Arrays.stream(values)
                                              .map(value -> new XAnnotatedReference(xmap, String.class, value, null, null))
                                              .toArray(XAnnotatedMember[]::new);
        init(xmap, setter, members, separator, defaultValue);
    }

    protected void init(XMap xmap, XAccessor<T> setter, XAnnotatedMember<T>[] members, String separator,
            String defaultValue) {
        this.members = members;
        this.separator = separator;
        this.defaultValue = defaultValue;
        if (setter != null) {
            type = setter.getType();
        } else {
            @SuppressWarnings("unchecked")
            Class<T> stringType = (Class<T>) String.class;
            type = stringType;
        }
        valueFactory = xmap.getValueFactory(type);
        xao = xmap.register(type);
    }

    @Override
    public boolean hasValue(Context ctx, Element element) {
        return Arrays.stream(members).anyMatch(member -> member.hasValue(ctx, element));
    }

    @Override
    public T getValue(Context ctx, Element base) {
        List<String> values = new ArrayList<>();
        for (XAnnotatedMember<T> member : members) {
            if (member.hasValue(ctx, base)) {
                Object mvalue = member.getValue(ctx, base);
                values.add(mvalue != null ? String.valueOf(mvalue) : "");
            }
        }
        if (values.isEmpty()) {
            return getDefaultValue(ctx);
        }
        @SuppressWarnings("unchecked")
        T joined = (T) String.join(separator, values);
        return joined;
    }

    @Override
    public void toXML(Object instance, Element parent) {
        Arrays.asList(members).forEach(member -> member.toXML(instance, parent));
    }

}
