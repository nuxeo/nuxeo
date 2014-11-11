/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for the registration of a QueryMaker.
 */
@XObject(value = "queryMaker")
public class QueryMakerDescriptor {

    @XNode("@name")
    public String name = "";

    @XNode("@enabled")
    public boolean enabled = true;

    @XNode(value="", trim=true)
    public Class<? extends QueryMaker> queryMaker;

}
