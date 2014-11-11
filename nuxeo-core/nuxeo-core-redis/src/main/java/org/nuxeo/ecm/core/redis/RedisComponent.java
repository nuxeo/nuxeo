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
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.osgi.framework.Bundle;

import redis.clients.jedis.Jedis;

/**
 * Implementation of the Redis Service holding the configured Jedis pool.
 *
 * @since 5.8
 */
public class RedisComponent extends DefaultComponent implements RedisAdmin {

    protected volatile RedisExecutor executor = RedisExecutor.NOOP;

    protected RedisPoolDescriptor config;

    protected SimpleContributionRegistry<RedisPoolDescriptor> registry =

    new SimpleContributionRegistry<RedisPoolDescriptor>() {

        @Override
        public String getContributionId(RedisPoolDescriptor contrib) {
            return "main";
        }

        @Override
        public void contributionUpdated(String id, RedisPoolDescriptor contrib,
                RedisPoolDescriptor newOrigContrib) {
            config = contrib;
        }

        @Override
        public void contributionRemoved(String id,
                RedisPoolDescriptor origContrib) {
            config = null;
        }
    };

    protected String delsha;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof RedisPoolDescriptor) {
            registry.addContribution((RedisPoolDescriptor) contribution);
        } else {
            super.registerContribution(contribution, extensionPoint,
                    contributor);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (config == null || config.disabled) {
            return;
        }
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                executor = null;
                executor.getPool().destroy();
            }
        });
        handleNewExecutor(config.newExecutor());
    }

    public void handleNewExecutor(RedisExecutor executor) {
        this.executor = executor;
        try {
            delsha = load("org.nuxeo.ecm.core.redis", "del-keys");
        } catch (IOException cause) {
            executor = null;
            throw new NuxeoException("Cannot activate redis executor", cause);
        }
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

    @Override
    public String namespace(String... names) {
        StringBuilder builder = new StringBuilder("nuxeo:");
        for (String name:names) {
            builder.append(name).append(":");
        }
        return builder.toString();
    }

}
