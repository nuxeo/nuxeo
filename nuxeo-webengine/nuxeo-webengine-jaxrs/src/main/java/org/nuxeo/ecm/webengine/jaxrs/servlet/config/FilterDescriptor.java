/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.util.HashMap;

import javax.servlet.Filter;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("filter")
public class FilterDescriptor {

    @XNode("@class")
    protected String classRef;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class, trim = true, nullByDefault = false)
    protected HashMap<String, String> initParams;

    private ClassRef ref;

    public ClassRef getClassRef() throws ClassNotFoundException, BundleNotFoundException {
        if (ref == null) {
            ref = Utils.getClassRef(classRef);
        }
        return ref;
    }

    public String getRawClassRef() {
        return classRef;
    }

    public Filter getFilter() throws ReflectiveOperationException, BundleNotFoundException {
        return (Filter) getClassRef().get().getDeclaredConstructor().newInstance();
    }

    public HashMap<String, String> getInitParams() {
        return initParams;
    }

    @Override
    public String toString() {
        return classRef + " " + initParams;
    }
}
