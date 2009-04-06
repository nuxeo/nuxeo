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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: GraphDescription.java 19245 2007-05-23 18:10:54Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.util.Map;

/**
 * Interface for a graph description.
 * <p>
 * A graph description gives all the information needed for the graph
 * instantiation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface GraphDescription {

    String getName();

    String getGraphType();

    Map<String, String> getOptions();

    Map<String, String> getNamespaces();

}
