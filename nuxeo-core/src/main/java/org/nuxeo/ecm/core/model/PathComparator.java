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

package org.nuxeo.ecm.core.model;

import java.util.Comparator;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class PathComparator implements Comparator<Document>, Serializable {

    private static final Log log = LogFactory.getLog(PathComparator.class);
    private static final long serialVersionUID = 3598980450344414494L;

    public int compare(Document o1, Document o2) {
        try {
            String path1 = o1.getPath();
            return path1.compareTo(o2.getPath());
        } catch (DocumentException e) {
            // can't throw again
            log.error("Failed getting a path from a Document instance!");
            return 0;
        }
    }

}
