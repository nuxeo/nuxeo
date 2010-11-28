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
 * $Id: PermissionUIItemComparator.java 28304 2007-12-21 12:13:32Z ogrisel $
 */
package org.nuxeo.ecm.core.security;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class PermissionUIItemComparator implements
        Comparator<PermissionUIItemDescriptor>, Serializable {

    private static final long serialVersionUID = 6468292882222351585L;

    @Override
    public int compare(PermissionUIItemDescriptor pid1,
            PermissionUIItemDescriptor pid2) {
        int diff = pid2.getOrder() - pid1.getOrder();
        if (diff == 0) {
            return 0;
        } else if (diff > 0) { // ascending order
            return -1;
        } else {
            return 1;
        }
    }

}
