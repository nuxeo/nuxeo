/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("unicitySettings")
public class UnicityExtension  implements Serializable {

    private static final long serialVersionUID = 7764225025169187266L;

    public static final List<String> DEFAULT_FIELDS = new ArrayList<String>();

    @XNode("algo")
    protected String algo;

    @XNode("enabled")
    protected Boolean enabled;

    @XNode("computeDigest")
    protected Boolean computeDigest = Boolean.FALSE;

    @XNodeList(value = "field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields = DEFAULT_FIELDS;

    public String getAlgo() {
        return algo;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public List<String> getFields() {
        return fields;
    }

    public Boolean getComputeDigest() {
        if (Boolean.TRUE.equals(enabled)) {
            return enabled;
        }
        return computeDigest;
    }

}
