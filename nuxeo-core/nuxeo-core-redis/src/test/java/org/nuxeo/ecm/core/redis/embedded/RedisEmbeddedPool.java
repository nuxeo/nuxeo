/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.embedded;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class RedisEmbeddedPool extends Pool<Jedis> {
    public RedisEmbeddedPool() {
        super(new GenericObjectPoolConfig(), new RedisEmbeddedFactory());
    }

    public void setError(RedisEmbeddedGuessConnectionError error) {
        ((RedisEmbeddedFactory)internalPool.getFactory()).error = error;
    }
}
