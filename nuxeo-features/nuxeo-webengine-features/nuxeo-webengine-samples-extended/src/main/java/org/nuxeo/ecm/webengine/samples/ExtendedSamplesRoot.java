/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * 
 * Demonstrates how to customize existing modules by 
 * contributing new views and links  {@link ExtendedDocumentsObject} for more precision 
 * 
 * @author matic
 *
 */
@WebObject(type = "ExtendedSamples", administrator = Access.GRANT)
@Path("/samples/extended")
public class ExtendedSamplesRoot extends SamplesRoot {

}
