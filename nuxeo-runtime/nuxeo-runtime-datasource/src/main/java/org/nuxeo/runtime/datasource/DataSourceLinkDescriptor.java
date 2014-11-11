/*******************************************************************************
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.datasource;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("link")
public class DataSourceLinkDescriptor {

	@XNode("@name") String name;
	
	@XNode("@global") String global;
	
	@XNode("@type") String type;
}
