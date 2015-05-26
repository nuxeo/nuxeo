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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedGuessConnectionError;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedPool;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedSynchronizedExecutor;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedTraceExecutor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import redis.clients.jedis.Protocol;

@Features({ CoreFeature.class, CacheFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class RedisFeature extends SimpleFeature {

    /**
     * This defines configuration that can be used to run Redis tests with a given Redis configured.
     *
     * @since 6.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Config {
        Mode mode() default Mode.undefined;

        String host() default "";

        int port() default 0;

        Class<? extends RedisEmbeddedGuessConnectionError> guessError() default RedisEmbeddedGuessConnectionError.NoError.class;
    }

    public static final String PROP_MODE = "nuxeo.test.redis.mode";

    public static final String PROP_HOST = "nuxeo.test.redis.host";

    public static final String PROP_PORT= "nuxeo.test.redis.port";

    public static final Mode DEFAULT_MODE = Mode.embedded;

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = Protocol.DEFAULT_PORT;

    public enum Mode {
        undefined, disabled, embedded, server, sentinel
    }

    protected Mode getMode() {
        Mode mode = config.mode();
        if (mode == Mode.undefined) {
            String modeProp = System.getProperty(PROP_MODE);
            if (StringUtils.isBlank(modeProp)) {
                mode = DEFAULT_MODE;
            } else {
                mode = Mode.valueOf(modeProp);
            }
        }
        return mode;
    }

    protected String getHost() {
        String host = config.host();
        if (StringUtils.isEmpty(host)) {
            String hostProp = System.getProperty(PROP_HOST);
            if (StringUtils.isBlank(hostProp)) {
                host = DEFAULT_HOST;
            } else {
                host = hostProp;
            }
        }
        return host;
    }

    protected int getPort() {
        int port = config.port();
        if (port == 0) {
            String portProp = System.getProperty(PROP_PORT);
            if (StringUtils.isBlank(portProp)) {
                port = DEFAULT_PORT;
            } else {
                port = Integer.parseInt(portProp);
            }
        }
   return port;
    }
    protected RedisServerDescriptor newRedisServerDescriptor() {
        RedisServerDescriptor desc = new RedisServerDescriptor();
        desc.host = getHost();
        desc.port = getPort();
        return desc;
    }

    protected RedisSentinelDescriptor newRedisSentinelDescriptor() {
        RedisSentinelDescriptor desc = new RedisSentinelDescriptor();
        desc.master = "mymaster";
        desc.hosts = new RedisHostDescriptor[] { new RedisHostDescriptor(getHost(), getPort()) };
        return desc;
    }

    public static void clear() throws IOException {
        final RedisAdmin admin = Framework.getService(RedisAdmin.class);
        admin.clear("*");
    }

    public static boolean setup(RuntimeHarness harness) throws Exception {
        return new RedisFeature().setupMe(harness);
    }

    protected boolean setupMe(RuntimeHarness harness) throws Exception {
        Mode mode = getMode();
        if (Mode.disabled.equals(mode)) {
            return false;
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
        harness.deployTestContrib("org.nuxeo.ecm.core.redis", RedisFeature.class.getResource("/redis-contribs.xml"));

        RedisComponent component = (RedisComponent) Framework.getRuntime().getComponent(
                RedisComponent.class.getPackage().getName());
        if (Mode.embedded.equals(mode)) {
            RedisExecutor executor = new RedisPoolExecutor(new RedisEmbeddedPool());
            executor = new RedisEmbeddedTraceExecutor(executor);
            executor = new RedisEmbeddedSynchronizedExecutor(executor);
            executor = new RedisFailoverExecutor(10, executor);
            component.handleNewExecutor(executor);
        } else {
            component.registerRedisPoolDescriptor(getDescriptor(mode));
            component.handleNewExecutor(component.getConfig().newExecutor());
        }

        clear();
        return true;
    }

    protected RedisPoolDescriptor getDescriptor(Mode mode) {
        switch (mode) {
        case sentinel:
            return newRedisSentinelDescriptor();
        case server:
            return newRedisServerDescriptor();
        default:
            break;
        }
        return null;
    }

    protected Config config = Defaults.of(Config.class);

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getConfig(Config.class);
        runner.getFeature(CacheFeature.class).enable();
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        setupMe(runner.getFeature(RuntimeFeature.class).getHarness());
    }

}
