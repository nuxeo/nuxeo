/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.mail;

import static org.nuxeo.mail.MailConstants.CONFIGURATION_JNDI_JAVA_MAIL;
import static org.nuxeo.mail.MailConstants.DEFAULT_MAIL_JNDI_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Composer {

    private static final Log log = LogFactory.getLog(Composer.class);

    /** Mail properties read from mail.properties. */
    protected static final Properties MAIL_PROPERTIES = new Properties();

    protected final FreemarkerEngine engine;

    protected Mailer mailer;

    // FIXME: don't put URLs in Maps, this is a serious performance issue.
    protected final ConcurrentMap<String, URL> urls;

    public Composer() {
        this(null);
    }

    public Composer(Mailer mailer) {
        urls = new ConcurrentHashMap<>();
        if (mailer == null) {
            this.mailer = createMailer();
        } else {
            this.mailer = mailer;
        }
        engine = new FreemarkerEngine();
        engine.setResourceLocator(new ResourceLocator() {
            @Override
            public URL getResourceURL(String key) {
                return urls.get(key);
            }

            @Override
            public File getResourceFile(String key) {
                return null;
            }
        });
    }

    protected Mailer createMailer() {
        // first try the local configuration
        // it was used by FakeSmtpMailServerFeature / EmbeddedAutomationClientTest#testSendMail - no runtime usage found
        var properties = getMailProperties();
        if (!properties.isEmpty()) {
            mailer = new Mailer(properties);
        }
        // second try using JNDI
        if (mailer == null) {
            String name = Framework.getProperty(CONFIGURATION_JNDI_JAVA_MAIL, DEFAULT_MAIL_JNDI_NAME);
            mailer = new Mailer(name);
        }
        return mailer;
    }

    @NotNull
    protected static Properties getMailProperties() {
        File mailFile = getMailPropertiesFile();
        if ((mailFile != null && MAIL_PROPERTIES.isEmpty()) || Framework.isTestModeSet()) {
            synchronized (MAIL_PROPERTIES) {
                if ((mailFile != null && MAIL_PROPERTIES.isEmpty()) || Framework.isTestModeSet()) {
                    MAIL_PROPERTIES.clear();
                    if (mailFile != null) {
                        try (FileInputStream in = new FileInputStream(mailFile)) {
                            MAIL_PROPERTIES.load(in);
                        } catch (IOException e) {
                            log.error("Failed to load mail properties", e);
                        }
                    }
                }
            }
        }
        return MAIL_PROPERTIES;
    }

    protected static File getMailPropertiesFile() {
        org.nuxeo.common.Environment env = org.nuxeo.common.Environment.getDefault();
        if (env != null) {
            File file = new File(env.getConfig(), "mail.properties");
            if (file.isFile()) {
                return file;
            }
        }
        return null;
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

    public void render(String template, Object ctx, Writer writer) throws RenderingException {
        engine.render(template, ctx, writer);
    }

    public void render(URL template, Object ctx, Writer writer) throws RenderingException {
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

    public String render(String templateContent, Object ctx) throws TemplateException, IOException {
        StringReader reader = new StringReader(templateContent);
        Template temp = new Template("@inline", reader, engine.getConfiguration(), "UTF-8");
        StringWriter writer = new StringWriter();
        Environment env = temp.createProcessingEnvironment(ctx, writer, engine.getObjectWrapper());
        env.process();
        return writer.toString();
    }

    public Mailer.Message newMessage() {
        return mailer.newMessage();
    }

    public Mailer.Message newTextMessage(URL template, Object ctx) throws RenderingException, MessagingException {
        Mailer.Message msg = mailer.newMessage();
        msg.setText(render(template, ctx), "UTF-8");
        return msg;
    }

    public Mailer.Message newTextMessage(String templateContent, Object ctx) throws RenderingException,
            MessagingException, TemplateException, IOException {
        Mailer.Message msg = mailer.newMessage();
        msg.setText(render(templateContent, ctx), "UTF-8");
        return msg;
    }

    public Mailer.Message newHtmlMessage(URL template, Object ctx) throws RenderingException, MessagingException {
        Mailer.Message msg = mailer.newMessage();
        msg.setContent(render(template, ctx), "text/html; charset=utf-8");
        return msg;
    }

    public Mailer.Message newHtmlMessage(String templateContent, Object ctx) throws MessagingException,
            TemplateException, IOException {
        Mailer.Message msg = mailer.newMessage();
        msg.setContent(render(templateContent, ctx), "text/html; charset=utf-8");
        return msg;
    }

    public Mailer.Message newMixedMessage(String templateContent, Object ctx, String textType, List<Blob> attachments)
            throws TemplateException, IOException, MessagingException {
        if (textType == null) {
            textType = "plain";
        }
        Mailer.Message msg = mailer.newMessage();
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart body = new MimeBodyPart();
        String result = render(templateContent, ctx);
        body.setText(result, "UTF-8", textType);
        mp.addBodyPart(body);
        for (Blob blob : attachments) {
            MimeBodyPart a = new MimeBodyPart();
            a.setDataHandler(new DataHandler(new BlobDataSource(blob)));
            a.setFileName(blob.getFilename());
            mp.addBodyPart(a);
        }
        msg.setContent(mp);
        return msg;
    }

}
