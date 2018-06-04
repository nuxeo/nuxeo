/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.codec;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@SuppressWarnings("CanBeFinal")
@XObject("codec")
public class CodecDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<CodecFactory> klass;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    public String getName() {
        return name;
    }

    public Class<CodecFactory> getKlass() {
        return klass;
    }

    @Override
    public String toString() {
        return "CodecDescriptor{" + "klass=" + klass + ", options=" + options + '}';
    }

    public CodecFactory getInstance() {
        try {
            CodecFactory ret = getKlass().getDeclaredConstructor().newInstance();
            ret.init(options);
            return ret;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid class: " + getClass(), e);
        }
    }
}
