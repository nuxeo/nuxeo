/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.core.redis.embedded;

import java.util.Random;

import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class RedisEmbeddedGuessConnectionError {

    protected final Thread ownerThread = Thread.currentThread();

    public static class NoError extends RedisEmbeddedGuessConnectionError {
        @Override
        protected void doGuessError() throws JedisConnectionException {
            return;
        }
    }

    public static class OnFirstCall extends RedisEmbeddedGuessConnectionError {

        protected boolean fired;

        @Override
        protected void doGuessError() throws JedisConnectionException {
            if (!fired) {
                fired = true;
                throw new JedisConnectionException("first call error");
            }
        }

    }

    public static class OnEveryCall extends RedisEmbeddedGuessConnectionError {

        @Override
        protected void doGuessError() throws JedisConnectionException {
            throw new JedisConnectionException("every call error");
        }

    }

    public static class OnRandomCall extends RedisEmbeddedGuessConnectionError {

        protected final Random random = new Random();

        @Override
        protected void doGuessError() throws JedisConnectionException {
            if (random.nextBoolean()) {
                throw new JedisConnectionException("random call error");
            }
        }

    }

    protected void guessError() {
        if (!onThread()) {
            return;
        }
        doGuessError();
    }

    protected abstract void doGuessError();

    protected boolean onThread() {
        return Thread.currentThread().equals(ownerThread);
    }
}
