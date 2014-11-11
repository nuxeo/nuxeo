/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.JbpmContext;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Filter of list.
 *
 * This filter allows to select only part of a list. It is used as parameter in
 * methods of {@link JbpmService}. Inside the filter method, the jbpmContext is
 * alive.
 *
 * @author arussel
 *
 */
public interface JbpmActorsListFilter {
    <T> ArrayList<T> filter(JbpmContext jbpmContext, DocumentModel document,
            ArrayList<T> list, List<String> actors);
}
