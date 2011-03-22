/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("login-module")
public class LoginModuleDescriptor {

    @XNode("@code")
    public Class<?> code;

    @XNode("@flag")
    public String flag;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, Object> options;

}
