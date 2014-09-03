/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis;

import java.util.concurrent.Callable;

import redis.clients.jedis.Jedis;

public abstract class RedisCallable<T> implements Callable<T> {

    protected Jedis jedis;

    protected String prefix;

}
