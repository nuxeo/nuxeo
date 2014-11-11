/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;

import freemarker.core.Environment;
import freemarker.template.Template;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Composer {

    private static final Log log = LogFactory.getLog(Composer.class);

    protected final FreemarkerEngine engine;

    protected Mailer mailer;

    protected final ConcurrentMap<String, URL> urls;

    public Composer() {
        this(null);
    }

    public Composer(Mailer mailer) {
        urls = new ConcurrentHashMap<String, URL>();
        if (mailer == null) {
            this.mailer = createMailer();
        } else {
            this.mailer = mailer;
        }
        engine = new FreemarkerEngine();
        engine.setResourceLocator(new ResourceLocator() {
            public URL getResourceURL(String key) {
                return urls.get(key);
            }

            public File getResourceFile(String key) {
                return null;
            }
        });
    }

    protected Mailer createMailer() {
        // first try the local configuration
        org.nuxeo.common.Environment env = org.nuxeo.common.Environment.getDefault();
        if (env != null) {
            File file = new File(env.getConfig(), "mail.properties");
            if (file.isFile()) {
                Properties p = new Properties();
                try {
                    FileInputStream in = new FileInputStream(file);
                    try {
                        p.load(in);
                        mailer = new Mailer(p);
                    } finally {
                        in.close();
                    }
                } catch (Exception e) {
                    log.error("Failed to load mail properties", e);
                }
            }
        }
        // second try using JNDI
        if (mailer == null) {
            String name = Framework.getProperty("jndi.java.mail", "java:/Mail");
            mailer = new Mailer(name);
        }
        return mailer;
    }

    public void registerTemplate(URL url) {
        urls.put(url.toExternalForm(), url);
    }

    public void unregisterTemplate(URL url) {
        urls.remove(url.toExternalForm());
    }

    public void unregisterAllTemplates() {
        urls.clear();
    }

    public Mailer getMailer() {
        return mailer;
    }

    public FreemarkerEngine getEngine() {
        return engine;
    }

    public void render(String template, Object ctx, Writer writer)
            throws RenderingException {
        engine.render(template, ctx, writer);
    }

    public void render(URL template, Object ctx, Writer writer)
            throws RenderingException {
        String key = template.toExternalForm();
        urls.putIfAbsent(key, template);
        engine.render(key, ctx, writer);
    }

    public String render(URL template, Object ctx) throws RenderingException {
        String key = template.toExternalForm();
        urls.putIfAbsent(key, template);
        StringWriter writer = new StringWriter();
        engine.render(key, ctx, writer);
        return writer.toString();
    }

    public String render(String templateContent, Object ctx) throws Exception {
        StringReader reader = new StringReader(templateContent);
        Template temp = new Template("@inline", reader,
                engine.getConfiguration(), "UTF-8");
        StringWriter writer = new StringWriter();
        Environment env = temp.createProcessingEnvironment(ctx, writer,
                engine.getObjectWrapper());
        env.process();
        return writer.toString();
    }

    public Mailer.Message newMessage() throws Exception {
        return mailer.newMessage();
    }

    public Mailer.Message newTextMessage(URL template, Object ctx)
            throws Exception {
        Mailer.Message msg = mailer.newMessage();
        msg.setText(render(template, ctx), "UTF-8");
        return msg;
    }

    public Mailer.Message newTextMessage(String templateContent, Object ctx)
            throws Exception {
        Mailer.Message msg = mailer.newMessage();
        msg.setText(render(templateContent, ctx), "UTF-8");
        return msg;
    }

    public Mailer.Message newHtmlMessage(URL template, Object ctx)
            throws Exception {
        Mailer.Message msg = mailer.newMessage();
        msg.setContent(render(template, ctx), "text/html");
        return msg;
    }

    public static void main(String[] args) throws Exception {
        Mailer mailer = new Mailer();
        mailer.setServer("smtp.gmail.com", "465", true);
        mailer.setCredentials("xxx", "xxx");
        mailer.setDebug(true);

        Composer c = new Composer(mailer);

        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "val1");

        Mailer.Message msg = c.newTextMessage("bla ${key1} bla", map).addFrom(
                "bs@nuxeo.com").addTo("bstefanescu@nuxeo.com");
        msg.setSubject("test2");
        msg.send();
    }

}
