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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisComponent extends DefaultComponent implements RedisAdmin {

    /**
     * @since 10.3
     */
    public static final String XP_CONFIG = "configuration";

    private static final String DEFAULT_PREFIX = "nuxeo:";

    protected volatile RedisExecutor executor;

    protected String delsha;

    @Override
    public RedisPoolDescriptor getConfig() {
        return getDescriptor(XP_CONFIG, UNIQUE_DESCRIPTOR_ID);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        RedisPoolDescriptor config = getConfig();
        if (config == null || config.disabled) {
            return;
        }
        this.executor = config.newExecutor();
        try {
            delsha = load("org.nuxeo.runtime.redis", "del-keys");
        } catch (RuntimeException cause) {
            this.executor = null;
            throw new RuntimeException("Cannot activate redis executor", cause);
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
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
        ComponentInstance compo = Framework.getRuntime().getComponentInstance("org.nuxeo.ecm.core.work.service");
        return compo != null ? ((DefaultComponent) compo.getInstance()).getApplicationStartedOrder() - 1 : -500;
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
        final StrBuilder builder;
        try (InputStream is = loc.openStream()) {
            builder = new StrBuilder();
            for (String line : IOUtils.readLines(is, UTF_8)) {
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
