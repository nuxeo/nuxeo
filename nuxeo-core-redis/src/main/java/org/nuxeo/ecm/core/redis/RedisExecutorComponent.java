/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisExecutorComponent extends DefaultComponent implements
        RedisConfiguration, RedisAdmin {

    protected static RedisExecutorComponent INSTANCE;

    protected SingletonContributionRegistry<RedisExecutorDescriptor> registry = new SingletonContributionRegistry<>();

    protected volatile RedisExecutor executor = RedisExecutor.NOOP;

    protected String delsha;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        INSTANCE = this;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        INSTANCE = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof RedisExecutorDescriptor) {
            registry.addContribution((RedisExecutorDescriptor) contribution);
        } else {
            super.registerContribution(contribution, extensionPoint,
                    contributor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof RedisPoolDescriptor) {
            registry.removeContribution((RedisExecutorDescriptor) contribution);
        } else {
            super.unregisterContribution(contribution, extensionPoint,
                    contributor);
        }
    }

    protected void handleNewPool(Pool<Jedis> pool) {
        executor = new RedisPoolExecutor(pool);
        if (registry.main.timeout > 0) {
            executor = new RedisFailoverExecutor(registry.main.timeout,
                    executor);
        }
        try {
            delsha = load("org.nuxeo.ecm.core.redis", "del-keys");
        } catch (IOException cause) {
            executor = null;
            throw new NuxeoException("Cannot activate redis executor", cause);
        }
    }

    protected void handlePoolDestroyed(Pool<Jedis> pool) {
        executor = RedisExecutor.NOOP;
    }

    @Override
    public String getPrefix() {
        return registry.main.prefix;
    }

    @Override
    public Long clear(final String pattern) throws IOException {
        return executor.execute(new RedisCallable<Long>() {

            @Override
            public Long call(Jedis jedis) throws Exception {
                List<String> keys = Arrays.asList(pattern);
                List<String> args = Arrays.asList();
                return (Long) jedis.evalsha(delsha, keys, args);
            }
        });
    }

    @Override
    public String load(String bundleName, String scriptName) throws IOException {
        Bundle b = Framework.getRuntime().getBundle(bundleName);
        URL loc = b.getEntry(scriptName + ".lua");
        InputStream is = loc.openStream();
        final StrBuilder builder = new StrBuilder();
        for (String line : IOUtils.readLines(is)) {
            builder.appendln(line);
        }
        return executor.execute(new RedisCallable<String>() {

            @Override
            public String call(Jedis jedis) throws Exception {
                return jedis.scriptLoad(builder.toString());
            }
        });
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(RedisExecutor.class)) {
            return adapter.cast(executor);
        }
        return super.getAdapter(adapter);
    }

}
