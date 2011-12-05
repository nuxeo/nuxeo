/*
 * (C) Copyright 2011 * All rights reserved. This program and the accompanying materials
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
 * $Id$
 */

package org.nuxeo.ecm.platform.task;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @since 5.5
 */
@XObject("taskPersister")
public class TaskPersisterDescriptor {

    @XNode("@path")
    private String path;

    public String getPath() {
        return path;
    }
}
