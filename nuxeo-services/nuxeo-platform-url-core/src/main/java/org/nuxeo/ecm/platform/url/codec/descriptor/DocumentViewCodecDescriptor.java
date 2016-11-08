/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentViewCodecDescriptor.java 22535 2007-07-13 14:57:58Z atchertchian $
 */

package org.nuxeo.ecm.platform.url.codec.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "documentViewCodec")
public class DocumentViewCodecDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String className;

    @XNode("@prefix")
    protected String prefix;

    @XNode("@default")
    protected boolean defaultCodec;

    @XNode("@enabled")
    protected boolean enabled;

    @XNode("@priority")
    protected int priority = 0;

    public String getClassName() {
        return className;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public boolean getDefaultCodec() {
        return defaultCodec;
    }

    public String getPrefix() {
        return prefix;
    }

}
