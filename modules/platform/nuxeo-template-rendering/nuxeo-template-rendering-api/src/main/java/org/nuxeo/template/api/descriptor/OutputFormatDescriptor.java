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

@XObject("outputFormat")
public class OutputFormatDescriptor {

    protected static final Log log = LogFactory.getLog(OutputFormatDescriptor.class);

    @XNode("@id")
    protected String id;

    @XNode("@label")
    protected String label;

    @XNode("@enabled")
    protected boolean enabled = true;

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

    public boolean isEnabled() {
        return enabled;
    }

    public String getChainId() {
        return chainId;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public OutputFormatDescriptor clone() {
        OutputFormatDescriptor clone = new OutputFormatDescriptor();
        clone.enabled = enabled;
        clone.chainId = chainId;
        clone.mimeType = mimeType;
        clone.label = label;
        clone.id = id;
        return clone;
    }

    public void merge(OutputFormatDescriptor srcOutFormat) {
        if (srcOutFormat.mimeType != null) {
            mimeType = srcOutFormat.mimeType;
        }
        if (srcOutFormat.chainId != null) {
            chainId = srcOutFormat.chainId;
        }
        if (srcOutFormat.label != null) {
            label = srcOutFormat.label;
        }
    }
}
