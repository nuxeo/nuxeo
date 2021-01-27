/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 *
 */
package org.nuxeo.template.api.descriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

@XObject("outputFormat")
@XRegistry(enable = false)
public class OutputFormatDescriptor {

    protected static final Log log = LogFactory.getLog(OutputFormatDescriptor.class);

    @XNode("@id")
    @XRegistryId
    protected String id;

    @XNode("@label")
    protected String label;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNode("@chainId")
    protected String chainId;

    @XNode("@mimetype")
    protected String mimeType;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getChainId() {
        return chainId;
    }

    public String getMimeType() {
        return mimeType;
    }

}
