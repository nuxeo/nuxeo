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
 *     arussel
 */
package org.nuxeo.ecm.platform.web.common.exceptionhandling.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.RequestDumper;

/**
 * @author arussel
 */
@XObject("requestdump")
public class RequestDumpDescriptor {

    @XNode("@class")
    Class<? extends RequestDumper> klass;

    @XNodeList(value = "notListed/attribute", type = ArrayList.class, componentType = String.class)
    List<String> attributes = new ArrayList<>();

    public Class<? extends RequestDumper> getKlass() {
        return klass;
    }

    public List<String> getAttributes() {
        return attributes;
    }

}
