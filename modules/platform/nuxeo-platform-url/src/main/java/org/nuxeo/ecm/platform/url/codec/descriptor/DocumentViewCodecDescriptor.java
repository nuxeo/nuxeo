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
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

@XObject(value = "documentViewCodec")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class DocumentViewCodecDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected String className;

    @XNode("@prefix")
    protected String prefix;

    @XNode("@default")
    protected boolean defaultCodec;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    public String getClassName() {
        return className;
    }

    public boolean getEnabled() {
        return enabled;
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
