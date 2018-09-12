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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import redis.clients.jedis.Protocol;

@Features({ RuntimeFeature.class })
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.redis")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.redis")
@Deploy("org.nuxeo.ecm.core.redis.tests:redis-contribs.xml")
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-default-workmanager-config.xml")
@Deploy("org.nuxeo.ecm.core.redis.tests:test-redis-workmanager.xml")
public class RedisFeature implements RunnerFeature {

    public static final String PROP_MODE = "nuxeo.test.redis.mode";

    public static final String PROP_HOST = "nuxeo.test.redis.host";

    public static final String PROP_PORT = "nuxeo.test.redis.port";

    public static final Mode DEFAULT_MODE = Mode.server;

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = Protocol.DEFAULT_PORT;

    public enum Mode {
        undefined, disabled, server, sentinel
    }

    protected Mode mode = Mode.undefined;

    protected String host = "";

    protected int port = 0;
    
    protected Mode getMode() {
        Mode mode = this.mode;
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
        String host = this.host;
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
        int port = this.port;
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

    @Override@Deploy("org.nuxeo.ecm.core.redis.tests:test-redis-workmanager.xml")
    public void start(FeaturesRunner runner) throws Exception {
        Framework.getRuntime().getComponentManager().addListener(new ComponentManager.Listener() {
            @Override
            public void afterActivation(ComponentManager mgr) {
                // overwrite the redis config (before redis component is started)
                RedisComponent comp = (RedisComponent) Framework.getRuntime().getComponent("org.nuxeo.ecm.core.redis");
                if (comp != null) {
                    Mode mode = getMode();
                    if (!Mode.disabled.equals(mode)) {
                        comp.registerContribution(getDescriptor(mode), RedisComponent.XP_CONFIG, null);
                    }
                }
            }
        });
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        final RedisAdmin admin = Framework.getService(RedisAdmin.class);
        admin.clear("*");
    }

    public boolean isRedisConfigured() {
        return !getMode().equals(Mode.disabled);
    }
}
