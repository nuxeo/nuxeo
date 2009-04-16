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
 * $Id: FilterFactory.java 20218 2007-06-07 19:19:46Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("custom-filter")
public class FilterFactory {

    // Instance variables are package-private because there are no accessors (for now?).

    @XNode("@id")
    String id;

    @XNode("@class")
    String className;

    @XNodeList(value = "action", type = String[].class, componentType = String.class)
    String[] acceptedActions = Action.EMPTY_CATEGORIES;

}
