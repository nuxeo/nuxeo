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
 * $Id: Annotation.java 20641 2007-06-17 13:13:22Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import java.io.Serializable;

/**
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public abstract class Annotation implements Serializable {

    private static final long serialVersionUID = -8194438499235331860L;

    public String id;

}
