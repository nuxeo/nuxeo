/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import junit.framework.Assert;

import org.junit.Assume;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import redis.clients.jedis.Protocol;

/**
 * This defines system properties that can be used to run Redis tests with a
 * given Redis host configured, independently of XML configuration.
 *
 * @since 5.8
 */
@Features({ CoreFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class RedisFeature extends SimpleFeature {

    private static final String REDIS_DEFAULT_MODE = "disabled";

    private static final int REDIS_SENTINEL_PORT_OFFSET = 20000;

    public static final String REDIS_MODE_PROP = "nuxeo.test.redis.mode";

    public static final String REDIS_HOST_PROP = "nuxeo.test.redis.host";

    public static final String REDIS_PORT_PROP = "nuxeo.test.redis.port";

    public static final String REDIS_SENTINEL_HOST_PROP = "nuxeo.test.redis.sentinel.host";

    public static final String REDIS_SENTINEL_PORT_PROP = "nuxeo.test.redis.sentinel.port";

    public static final String REDIS_PREFIX_PROP = "nuxeo.test.redis.prefix";

    public enum Mode {
        disabled, server, sentinel
    }

    public static Mode getMode() {
        return Mode.valueOf(Framework.getProperty(REDIS_MODE_PROP,
                REDIS_DEFAULT_MODE));
    }

    public static String getHost() {
        return Framework.getProperty(REDIS_HOST_PROP, "localhost");
    }

    public static int getPort() {
        String port = Framework.getProperty(REDIS_PORT_PROP,
                Integer.toString(Protocol.DEFAULT_PORT));
        return Integer.parseInt(port);
    }

    public static String getSentinelHost() {
        return Framework.getProperty(REDIS_SENTINEL_HOST_PROP, "localhost");
    }

    public static int getSentinelPort() {
        String port = Framework.getProperty(
                REDIS_SENTINEL_PORT_PROP,
                Integer.toString(REDIS_SENTINEL_PORT_OFFSET
                        + Protocol.DEFAULT_PORT));
        return Integer.parseInt(port);
    }

    public static String getPrefix() {
        return Framework.getProperty(REDIS_PREFIX_PROP, "nuxeo:test:");
    }

    public static RedisConfigurationDescriptor getRedisDescriptor() {
        switch (getMode()) {
        case server:
            return getRedisServerDescriptor();
        case sentinel:
            return getRedisSentinelDescriptor();
        default:
            return getRedisDisabledDescriptor();
        }
    }

    public static RedisConfigurationDescriptor getRedisDisabledDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.disabled = true;
        desc.prefix = "nuxeo:test";
        return desc;
    }

    public static RedisConfigurationDescriptor getRedisServerDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                getHost(), getPort()) };
        desc.prefix = getPrefix();
        return desc;
    }

    public static RedisConfigurationDescriptor getRedisSentinelDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.master = "mymaster";
        desc.hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                getSentinelHost(), getSentinelPort()) };
        desc.prefix = getPrefix();
        return desc;
    }

    public static void clearRedis(RedisConfiguration redisService)
            throws IOException {
        Framework.getService(RedisServiceImpl.class).clear();
    }

    public static void setup(RuntimeHarness harness) throws Exception {
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.ecm.core.event") == null) {
            harness.deployBundle("org.nuxeo.ecm.core.event");
        }
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.ecm.core.storage") == null) {
            harness.deployBundle("org.nuxeo.ecm.core.storage");
        }
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.ecm.core.cache") == null) {
            harness.deployBundle("org.nuxeo.ecm.core.cache");
        }
        harness.deployBundle("org.nuxeo.ecm.core.redis");
        harness.deployTestContrib("org.nuxeo.ecm.core.redis",
                RedisFeature.class.getResource("/redis-contribs.xml"));
        final RedisConfigurationDescriptor config = getRedisDescriptor();
        Assume.assumeTrue(!config.disabled);
        final RedisServiceImpl redis = Framework.getService(RedisServiceImpl.class);
        if (!redis.activate(config)) {
            Assert.fail("Cannot configure redis pool");
        }
        redis.clear();
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(CacheFeature.class).enable();
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        setup(runner.getFeature(RuntimeFeature.class).getHarness());
    }

}
