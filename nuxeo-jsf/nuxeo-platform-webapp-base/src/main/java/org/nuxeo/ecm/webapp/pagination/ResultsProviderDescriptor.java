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

package org.nuxeo.ecm.webapp.pagination;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;

/**
 * Registration unit to bind a {@link} PagedDocumentsProvider name
 * to the {@link ResultsProviderFarm} that's able to build it.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@XObject(value = "resultsProvider")
public class ResultsProviderDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@farm")
    protected String farm;

    public String getName() {
        return name;
    }

    public String getFarm() {
        return farm;
    }

}
