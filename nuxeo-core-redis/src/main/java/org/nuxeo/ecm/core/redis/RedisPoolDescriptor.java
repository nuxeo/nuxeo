/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import redis.clients.jedis.Protocol;

@XObject("pool")
public abstract class RedisPoolDescriptor {

    @XNode("disabled")
    protected boolean disabled = false;

    public String password;

    @XNode("password")
    public void setPassword(String value) {
        password = StringUtils.defaultIfBlank(value, null);
    }

    @XNode("database")
    public int database = Protocol.DEFAULT_DATABASE;

    @XNode("timeout")
    public int timeout = Protocol.DEFAULT_TIMEOUT;

    protected abstract RedisExecutor newExecutor();
}
