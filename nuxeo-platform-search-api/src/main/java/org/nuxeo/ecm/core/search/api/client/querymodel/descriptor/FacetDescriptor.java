/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.api.client.querymodel.descriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "field")
@Deprecated
public class FacetDescriptor {

    private static final Log log = LogFactory.getLog(FacetDescriptor.class);

    @XNode("@required")
    protected boolean required;

    @XNode("@name")
    protected final String setName() {
        String msg = "Facet post filtering has been replaced by "
                + "the \"ecm:mixinType\" query pseudo-field";
        log.error(msg);
        throw new RuntimeException(msg);
    }

    public String getName() {
        return null;
    }

    public boolean isRequired() {
        return required;
    }

}
