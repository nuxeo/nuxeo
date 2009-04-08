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
 * $Id: DBAnnotatableDocument.java 20672 2007-06-17 14:11:55Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public interface DBAnnotatableDocument {

    Object getAnnotation(String name) throws ClientException;

    void setAnnotation(String name, Object value) throws ClientException;

}
