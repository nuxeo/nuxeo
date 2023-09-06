/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.nuxeo.mail;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Describes a {@link MailSender} which can be parameterized with properties.
 *
 * @since 2023.3
 */
@XObject("sender")
public class MailSenderDescriptor implements Descriptor {

    @XNode("@class")
    public Class<? extends MailSender> klass;

    @XNode("@name")
    protected String name;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties;

    @Override
    public String getId() {
        return name;
    }

    public MailSender newInstance() {
        if (!MailSender.class.isAssignableFrom(klass)) {
            throw new IllegalArgumentException(
                    "Cannot instantiate class: " + klass + ", class must implement MailSender");
        }
        try {
            return klass.getDeclaredConstructor(MailSenderDescriptor.class).newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot instantiate: " + klass, e);
        }
    }

}
