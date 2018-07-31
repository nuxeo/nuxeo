/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import redis.clients.jedis.Protocol;

@Features({ RuntimeFeature.class })
public class RedisFeature implements RunnerFeature {

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
    }

    public static final String PROP_MODE = "nuxeo.test.redis.mode";

    public static final String PROP_HOST = "nuxeo.test.redis.host";

    public static final String PROP_PORT = "nuxeo.test.redis.port";

    public static final Mode DEFAULT_MODE = Mode.server;

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = Protocol.DEFAULT_PORT;

    public enum Mode {
        undefined, disabled, server, sentinel
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
        desc.hosts = new RedisSentinelDescriptor.RedisHostDescriptor[] {
                new RedisSentinelDescriptor.RedisHostDescriptor(getHost(), getPort()) };
        return desc;
    }

    public static void clear() {
        final RedisAdmin admin = Framework.getService(RedisAdmin.class);
        admin.clear("*");
    }

    public static boolean setup(RuntimeHarness harness) throws Exception {
        RedisFeature redisFeature = setUpFeature(harness);
        return !Mode.disabled.equals(redisFeature.getMode());
    }

    public static RedisFeature setUpFeature(RuntimeHarness harness) throws Exception {
        RedisFeature redisFeature = new RedisFeature();
        redisFeature.setupMe(harness);
        return redisFeature;
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
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.runtime.redis") == null) {
            harness.deployBundle("org.nuxeo.runtime.redis");
        }
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.runtime.kv") == null) {
            harness.deployBundle("org.nuxeo.runtime.kv");
        }
        if (harness.getOSGiAdapter().getBundle("org.nuxeo.ecm.core.cache") == null) {
            harness.deployBundle("org.nuxeo.ecm.core.cache");
        }
        harness.deployBundle("org.nuxeo.ecm.core.redis");
        harness.deployContrib("org.nuxeo.ecm.core.redis.tests", "redis-contribs.xml");

        registerComponentListener();// this will dynamically configure redis component when activated
        return true;
    }

    protected boolean installConfig(RedisComponent component) {
        Mode mode = getMode();
        if (Mode.disabled.equals(mode)) {
            return false;
        }
        component.registerContribution(getDescriptor(mode), RedisComponent.XP_CONFIG, null);
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

    protected void registerComponentListener() {
        Framework.getRuntime().getComponentManager().addListener(new ComponentManager.Listener() {
            @Override
            public void afterActivation(ComponentManager mgr) {
                // overwrite the redis config (before redis component is started)
                RedisComponent comp = (RedisComponent) Framework.getRuntime().getComponent("org.nuxeo.ecm.core.redis");
                if (comp != null) {
                    installConfig(comp);
                }
            }
        });
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        config = runner.getConfig(Config.class);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        setupMe(runner.getFeature(RuntimeFeature.class).getHarness());
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        clear();
    }

}
