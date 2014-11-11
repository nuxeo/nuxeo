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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedGuessConnectionError;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedPool;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedSynchronizedExecutor;
import org.nuxeo.ecm.core.redis.embedded.RedisEmbeddedTraceExecutor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.test.InlineRef;
import org.nuxeo.runtime.test.protocols.inline.InlineURLFactory;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Features({ CoreFeature.class, CacheFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class RedisFeature extends SimpleFeature {

    /**
     * This defines configuration that can be used to run Redis tests with a
     * given Redis configured.
     *
     * @since 6.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface Config {
        Mode mode() default Mode.embedded;

        String host() default "localhost";

        int port() default 0;

        String prefix() default "nuxeo:test:";

        long failoverTimeout() default -1;

        Class<? extends RedisEmbeddedGuessConnectionError> guessError() default RedisEmbeddedGuessConnectionError.NoError.class;
    }

    public enum Mode {
        disabled, embedded, server, sentinel
    }

    public RedisFeature() {

    }

    public RedisFeature(Config config) {
        this.config = config;
    }

    protected RedisServerDescriptor newRedisServerDescriptor() {
        RedisServerDescriptor desc = new RedisServerDescriptor();
        desc.hosts = new RedisHostDescriptor[] { new RedisHostDescriptor(
                config.host(), config.port()) };
        return desc;
    }

    protected RedisSentinelDescriptor newRedisSentinelDescriptor() {
        RedisSentinelDescriptor desc = new RedisSentinelDescriptor();
        desc.master = "mymaster";
        desc.hosts = new RedisHostDescriptor[] { new RedisHostDescriptor(
                config.host(), config.port()) };
        return desc;
    }

    public static void clear()
            throws IOException {
        final RedisAdmin admin = Framework.getService(RedisAdmin.class);
        admin.clear("*");
    }

    public static boolean setup(RuntimeHarness harness) throws Exception {
        return new RedisFeature(Defaults.of(Config.class)).setupMe(harness);
    }

    protected boolean setupMe(RuntimeHarness harness) throws Exception {
        if (Mode.disabled.equals(config.mode())) {
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
        harness.deployTestContrib("org.nuxeo.ecm.core.redis",
                RedisFeature.class.getResource("/redis-contribs.xml"));


        if (Mode.embedded.equals(config.mode())) {
            RedisComponent component = (RedisComponent) Framework.getRuntime().getComponent(RedisComponent.class.getPackage().getName());
            RedisExecutor executor = new RedisPoolExecutor(new RedisEmbeddedPool());
            executor = new RedisEmbeddedTraceExecutor(executor);
            executor = new RedisEmbeddedSynchronizedExecutor(executor);
            executor = new RedisFailoverExecutor(10, executor);
            component.handleNewExecutor(executor);
        } else {
            RuntimeContext context = Framework.getRuntime().getContext();
            context.deploy(toDescriptor(config));
        }

        clear();
        return true;
    }

    private InlineRef toDescriptor(Config config) throws IOException,
            URISyntaxException, ParserConfigurationException {
        File sourceFile = new File(RedisFeature.class.getResource(
                "/redis-config.xml").toURI());
        TextTemplate template = new TextTemplate();
        template.setVariable("timeout", Long.toString(config.failoverTimeout()));
        RedisServerDescriptor desc = null;
        XMap xmap = new XMap();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element extension = (Element)doc.appendChild(doc.createElement("extension"));
        extension.setAttribute("target", "org.nuxeo.ecm.core.redis.RedisConnector");
        extension.setAttribute("point", "configuration");
        switch (config.mode()) {
        case sentinel:
            xmap.register(RedisSentinelDescriptor.class);
            desc = newRedisSentinelDescriptor();
            break;
        case server:
            extension.setAttribute("target", "org.nuxeo.ecm.core.redis.RedisSentinel");
            xmap.register(RedisServerDescriptor.class);
            desc = newRedisServerDescriptor();
            break;
        default:
            break;
        }
        String ext = "";
        if (desc != null) {
            xmap.toXML(desc, doc.getDocumentElement());
            ext = DOMSerializer.toStringOmitXml((Element)doc.getDocumentElement().getChildNodes().item(0));
        }
        template.setVariable("extension", ext);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        template.process(new FileInputStream(sourceFile), bos);
        return new InlineRef("redis-test", bos.toString());
    }

    protected Config config;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getConfig(Config.class);
        runner.getFeature(CacheFeature.class).enable();
        InlineURLFactory.install();
    }


    @Override
    public void start(FeaturesRunner runner) throws Exception {
        setupMe(runner.getFeature(RuntimeFeature.class).getHarness());
    }

}
