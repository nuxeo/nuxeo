/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.io.IOException;

import redis.clients.jedis.exceptions.JedisException;

public interface RedisExecutor {

    /**
     * Invoke the jedis statement
     *
     * @since 5.9.6
     */
    <T> T execute(RedisCallable<T> call) throws IOException, JedisException;
}
