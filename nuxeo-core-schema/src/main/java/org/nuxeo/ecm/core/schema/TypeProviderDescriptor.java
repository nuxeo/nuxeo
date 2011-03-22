/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.ServiceLocatorFactory;

/**
 * A object describing a type provider.
 * <p>
 * A type provider is useful to import types from remote servers and it is
 * described by an URI or a service group.
 * <p>
 * I an URI is given, it will be used to lookup the type provider service using
 * a service locator as returned by {@link ServiceLocatorFactory}.
 * <p>
 * If a service group is given the service locator bound to the group will be
 * used to locate the service
 * <p>
 * If both the URI and group are defined, the URI will take precedence.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("provider")
public class TypeProviderDescriptor {

    @XNode("@uri")
    public String uri;

    @XNode("@group")
    public String group;

}
