/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.utils;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
/**
* @author Guillaume Cusnieux
*/
public class NuxeoGadgetFilter implements Filter {

	private static final long serialVersionUID = 1L;
	private static final Object PICTBOOK = "Picturebook";
	private static final Object GED = "Ged";
	private static final Object AGENDA = "Agenda";

	public boolean accept(DocumentModel doc) {

		return GED.equals(doc.getType()) || PICTBOOK.equals(doc.getType())
				|| AGENDA.equals(doc.getType());
	}

}
