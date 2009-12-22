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
 * $Id: QueryResult.java 19480 2007-05-27 10:46:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Query result interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface QueryResult extends Serializable {

    Integer getCount();

    void setCount(Integer count);

    List<Map<String, Node>> getResults();

    void setResults(List<Map<String, Node>> results);

    List<String> getVariableNames();

    void setVariableNames(List<String> variableNames);

}
