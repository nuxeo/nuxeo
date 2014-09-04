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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Pipeline;
import redis.clients.util.Pool;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisServiceImpl extends DefaultComponent implements
        RedisConfiguration {

    private static final Log log = LogFactory.getLog(RedisServiceImpl.class);

    public static final String DEFAULT_PREFIX = "nuxeo:";

    protected final ConfigRegistry configRegistry = new ConfigRegistry();

    protected class ConfigRegistry extends
            SimpleContributionRegistry<RedisConfigurationDescriptor> {

        @Override
        public String getContributionId(RedisConfigurationDescriptor contrib) {
            return "main";
        }

        protected RedisConfigurationDescriptor getDescriptor() {
            return currentContribs.get("main");
        }

        @Override
        public void contributionUpdated(String id,
                RedisConfigurationDescriptor contrib,
                RedisConfigurationDescriptor newOrigContrib) {
            if (contrib.disabled) {
                deactivate();
            } else {
                activate(contrib);
            }
        }

        @Override
        public void contributionRemoved(String id,
                RedisConfigurationDescriptor origContrib) {
            deactivate();
        }
    }

    protected RedisConfigurationDescriptor config;

    protected Pool<Jedis> pool;

    protected RedisExecutor executor;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("configuration".equals(extensionPoint)) {
            configRegistry.addContribution((RedisConfigurationDescriptor) contribution);
            return;
        }
        throw new RuntimeException("Unknown extension point : "
                + extensionPoint);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("configuration".equals(extensionPoint)) {
            configRegistry.removeContribution((RedisConfigurationDescriptor) contribution);
            return;
        }
        log.warn("Unknown extension point : " + extensionPoint);
    }

    protected ComponentContext context;

    @Override
    public void activate(ComponentContext context) throws Exception {
        this.context = context;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        if (config != null) {
            deactivate(config);
        }
        this.context = null;
    }

    public boolean activate(RedisConfigurationDescriptor desc) {
        log.info("Registering Redis configuration");
        deactivate();
        if (desc.disabled) {
            return false;
        }
        if (desc.hosts.length == 0) {
            throw new RuntimeException("Missing Redis host");
        }
        if (!canConnect(desc.hosts)) {
            log.info("Disabling redis, cannot connect to server");
            return false;
        }
        config = desc;
        if (!desc.isSentinel()) {
            pool = new JedisPool(new JedisPoolConfig(), desc.hosts[0].name,
                    desc.hosts[0].port, desc.timeout,
                    StringUtils.defaultIfBlank(desc.password, null),
                    desc.database);
            executor = new RedisPoolExecutor(pool, config.prefix);
        } else {
            try {
                pool = new JedisSentinelPool(desc.master,
                        toSentinels(desc.hosts), new JedisPoolConfig(),
                        desc.timeout, StringUtils.defaultIfBlank(desc.password,
                                null), desc.database);
                executor = new RedisFailoverExecutor(config.timeout,
                        new RedisPoolExecutor(pool, config.prefix));
            } catch (Exception cause) {
                pool = null;
                config = null;
                throw new RuntimeException("Cannot connect to redis", cause);
            }
        }
        return true;
    }

    protected boolean canConnect(RedisConfigurationHostDescriptor[] hosts) {
        for (RedisConfigurationHostDescriptor host : hosts) {
            if (canConnect(host.name, host.port)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canConnect(String name, int port) {
        try (Jedis jedis = new Jedis(name, port)) {
            return canPing(jedis);
        }
    }

    protected boolean canPing(Jedis jedis) {
        try {
            String pong = jedis.ping();
            return "PONG".equals(pong);
        } catch (Exception cause) {
            return false;
        }
    }

    protected Set<String> toSentinels(RedisConfigurationHostDescriptor[] hosts) {
        Set<String> sentinels = new HashSet<String>();
        for (RedisConfigurationHostDescriptor host : hosts) {
            sentinels.add(host.name + ":" + host.port);
        }
        return sentinels;
    }

    public void deactivate(RedisConfigurationDescriptor desc) {
        log.info("Unregistering Redis configuration");
        deactivate();
    }

    protected void deactivate() {
        if (pool == null) {
            return;
        }
        try {
            pool.destroy();
        } finally {
            pool = null;
            config = null;
        }
    }

    @Override
    public String getPrefix() {
        if (config == null) {
            return null;
        }
        String prefix = config.prefix;
        if ("NULL".equals(prefix)) {
            prefix = "";
        } else if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_PREFIX;
        }
        return prefix;
    }

    protected static class DelKeys extends RedisCallable<Void> {

        @Override
        public Void call() {
            Set<String> keys = jedis.keys(prefix + "*");
            Pipeline pipe = jedis.pipelined();
            for (String key : keys) {
                pipe.del(key);
            }
            pipe.sync();
            return null;
        }
    }

    public void clear() throws IOException {
        executor.execute(new DelKeys());
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(RedisExecutor.class)) {
            return adapter.cast(executor);
        }
        return super.getAdapter(adapter);
    }

}
