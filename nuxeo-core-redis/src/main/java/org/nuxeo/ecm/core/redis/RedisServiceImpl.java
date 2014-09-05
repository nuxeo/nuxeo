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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
            deactivate();
        }
        this.context = null;
    }

    protected String delsha;

    public boolean activate(RedisConfigurationDescriptor desc) {
        log.info("Registering Redis configuration");
        deactivate();
        if (desc.disabled) {
            return false;
        }
        if (!desc.start()) {
            return false;
        }
        config = desc;
        try {
            delsha = load("org.nuxeo.ecm.core.redis", "del-keys");
        } catch (IOException cause) {
            deactivate();
            return false;
        }
        return true;
    }

    protected void deactivate() {
        if (config == null) {
            return;
        }
        try {
            config.stop();
        } finally {
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

    public Long clear(final String pattern) throws IOException {
        return config.executor.execute(new RedisCallable<Long>() {

            @Override
            public Long call() throws Exception {
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
        return config.executor.execute(new RedisCallable<String>() {

            @Override
            public String call() throws Exception {
                return jedis.scriptLoad(builder.toString());
            }
        });
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(RedisExecutor.class)) {
            return adapter.cast(config.executor);
        }
        return super.getAdapter(adapter);
    }

}
