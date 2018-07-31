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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.server;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 5.8
 */
@XObject("marshaller")
public class MarshallerDescriptor implements Descriptor {

    @XNodeList(value = "writer", componentType = Class.class, type = ArrayList.class)
    public List<Class<? extends MessageBodyWriter<?>>> writers = new ArrayList<>();

    @XNodeList(value = "reader", componentType = Class.class, type = ArrayList.class)
    public List<Class<? extends MessageBodyReader<?>>> readers = new ArrayList<>();

    @Override
    public String getId() {
        return toString();
    }

}
