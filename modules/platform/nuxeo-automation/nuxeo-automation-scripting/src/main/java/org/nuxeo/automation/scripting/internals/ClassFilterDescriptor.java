/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.automation.scripting.internals;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for classes that may be allowed or denied for use by the scripting engine.
 *
 * @since 10.2
 */
@XObject("classFilter")
public class ClassFilterDescriptor {

    @XNodeList(value = "allow", type = ArrayList.class, componentType = String.class)
    protected List<String> allow;

    @XNodeList(value = "deny", type = ArrayList.class, componentType = String.class)
    protected List<String> deny;

}
