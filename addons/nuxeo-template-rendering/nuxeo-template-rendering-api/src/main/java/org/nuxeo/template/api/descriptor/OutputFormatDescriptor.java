/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ldoguin
 *
 */
package org.nuxeo.template.api.descriptor;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("outputFormat")
public class OutputFormatDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

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
