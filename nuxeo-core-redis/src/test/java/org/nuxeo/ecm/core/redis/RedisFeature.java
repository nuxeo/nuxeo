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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedPool;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.InlineRef;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

@Features({ CoreFeature.class, CacheFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class RedisFeature extends SimpleFeature {

    /**
     * This defines configuration that can be used to run Redis tests with a
     * given Redis configured.
     *
     * @since 5.9.6
     */
    public @interface Config {
        Mode mode() default Mode.embedded;

        String host() default "localhost";

        int port() default 0;

        String prefix() default "nuxeo:test:";

        long failoverTimeout() default -1;
    }

    public enum Mode {
        disabled, embedded, server, sentinel
    }

    public RedisFeature() {

    }

    public RedisFeature(Config config) {
        this.config = config;
    }

    protected RedisConfigurationDescriptor getRedisDisabledDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.disabled = true;
        return desc;
    }

    protected RedisConfigurationDescriptor getRedisServerDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                config.host(), config.port()) };
        desc.prefix = config.prefix();
        return desc;
    }

    protected RedisConfigurationDescriptor getRedisSentinelDescriptor() {
        RedisConfigurationDescriptor desc = new RedisConfigurationDescriptor();
        desc.master = "mymaster";
        desc.hosts = new RedisConfigurationHostDescriptor[] { new RedisConfigurationHostDescriptor(
                config.host(), config.port()) };
        desc.prefix = config.prefix();
        return desc;
    }

    public static void clearRedis(RedisConfiguration redisService)
            throws IOException {
        Framework.getService(RedisServiceImpl.class).clear(
                redisService.getPrefix().concat("*"));
    }

    public static void setup(RuntimeHarness harness) throws Exception {
        new RedisFeature(Defaults.of(Config.class)).setupMe(harness);
    }

    protected void setupMe(RuntimeHarness harness) throws Exception {
        if (Mode.disabled.equals(config.mode())) {
            return;
        }
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
        if (Mode.embedded.equals(config.mode())) {
            RedisServiceImpl redis = Framework.getService(RedisServiceImpl.class);
            redis.handleNewExecutor(newEmbeddedExecutor());
        } else {
            RuntimeContext context = Framework.getRuntime().getContext();
            context.deploy(toDescriptor(config));
        }
    }

    private InlineRef toDescriptor(Config config) throws IOException,
            URISyntaxException {
        File sourceFile = new File(RedisFeature.class.getResource(
                "/redis-config.xml").toURI());

        RedisConfigurationDescriptor desc = null;
        switch (config.mode()) {
        case sentinel:
            desc = getRedisSentinelDescriptor();
            break;
        case server:
            desc = getRedisServerDescriptor();
            break;
        default:
            throw new IllegalArgumentException(
                    "unsupported descriptor serialization for " + config.mode());
        }
        XMap xmap = new XMap();
        xmap.register(RedisConfigurationDescriptor.class);
        TextTemplate template = new TextTemplate();
        template.setVariable("descriptor", xmap.toXML(desc));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        template.process(new FileInputStream(sourceFile), bos);
        return new InlineRef("redis-test", bos.toString());
    }

    protected RedisExecutor newEmbeddedExecutor() {
        Pool<Jedis> pool = new RedisEmbeddedPool(new GenericObjectPoolConfig());
        RedisExecutor executor = new RedisPoolExecutor(pool);
        if (config.failoverTimeout() > 0) {
            executor = new RedisFailoverExecutor(config.failoverTimeout(),
                    executor);
        }
        return executor;
    }

    protected Config config;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getConfig(Config.class);
        runner.getFeature(CacheFeature.class).enable();
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        setupMe(runner.getFeature(RuntimeFeature.class).getHarness());
    }

    public void setFailover() {
        switch (config.mode()) {
        case disabled:
            break;
        case sentinel:
        case server:
            throw new IllegalStateException("Cannot run failover test in mode "
                    + config.mode());
        case embedded:
            ;
        }

    }

}
