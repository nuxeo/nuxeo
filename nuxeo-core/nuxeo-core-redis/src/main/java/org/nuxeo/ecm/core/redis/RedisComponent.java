/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.osgi.framework.Bundle;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisComponent extends DefaultComponent implements RedisAdmin {

    private static final String DEFAULT_PREFIX = "nuxeo:";

    protected volatile RedisExecutor executor;

    protected RedisPoolDescriptorRegistry registry = new RedisPoolDescriptorRegistry();

    public static class RedisPoolDescriptorRegistry extends SimpleContributionRegistry<RedisPoolDescriptor> {

        protected RedisPoolDescriptor config;

        @Override
        public String getContributionId(RedisPoolDescriptor contrib) {
            return "main";
        }

        @Override
        public void contributionUpdated(String id, RedisPoolDescriptor contrib, RedisPoolDescriptor newOrigContrib) {
            config = contrib;
        }

        @Override
        public void contributionRemoved(String id, RedisPoolDescriptor origContrib) {
            config = null;
        }

        public RedisPoolDescriptor getConfig() {
            return config;
        }

        public void clear() {
            config = null;
        }
    };

    protected String delsha;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        registry.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof RedisPoolDescriptor) {
            registerRedisPoolDescriptor((RedisPoolDescriptor) contribution);
        } else {
            throw new NuxeoException("Unknown contribution class: " + contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (contribution instanceof RedisPoolDescriptor) {
            unregisterRedisPoolDescriptor((RedisPoolDescriptor) contribution);
        }
    }

    public void registerRedisPoolDescriptor(RedisPoolDescriptor contrib) {
        registry.addContribution(contrib);
    }

    public void unregisterRedisPoolDescriptor(RedisPoolDescriptor contrib) {
        registry.removeContribution(contrib);
    }

    @Override
    public RedisPoolDescriptor getConfig() {
        return registry.getConfig();
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        RedisPoolDescriptor config = getConfig();
        if (config == null || config.disabled) {
            return;
        }
        handleNewExecutor(config.newExecutor());
    }

    @Override
    public void applicationStopped(ComponentContext context, Instant deadline) {
        if (executor == null) {
            return;
        }
        try {
            executor.getPool().destroy();
        } finally {
            executor = null;
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return ((DefaultComponent) Framework.getRuntime().getComponentInstance("org.nuxeo.ecm.core.work.service").getInstance()).getApplicationStartedOrder() - 1;
    }

    public void handleNewExecutor(RedisExecutor executor) {
        this.executor = executor;
        try {
            delsha = load("org.nuxeo.ecm.core.redis", "del-keys");
        } catch (RuntimeException cause) {
            executor = null;
            throw new NuxeoException("Cannot activate redis executor", cause);
        }
    }

    @Override
    public Long clear(final String pattern) {
        return (Long) executor.evalsha(delsha, Collections.singletonList(pattern), Collections.emptyList());
    }

    @Override
    public String load(String bundleName, String scriptName) {
        Bundle b = Framework.getRuntime().getBundle(bundleName);
        URL loc = b.getEntry(scriptName + ".lua");
        if (loc == null) {
            throw new RuntimeException("Fail to load lua script: " + scriptName);
        }
        InputStream is = null;
        final StrBuilder builder;
        try {
            is = loc.openStream();
            builder = new StrBuilder();
            for (String line : IOUtils.readLines(is)) {
                builder.appendln(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to load lua script: " + scriptName, e);
        }

        return executor.scriptLoad(builder.toString());
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(RedisExecutor.class)) {
            return adapter.cast(executor);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public String namespace(String... names) {
        RedisPoolDescriptor config = getConfig();
        String prefix = config == null ? null : config.prefix;
        if (StringUtils.isBlank(prefix)) {
            prefix = DEFAULT_PREFIX;
        }
        StringBuilder builder = new StringBuilder(prefix);
        for (String name : names) {
            builder.append(name).append(":");
        }
        return builder.toString();
    }

}
