package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import java.io.File;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.mail.Mailer;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.IResultPublisher;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.PublishByMail;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit",
/* following bundles are required to be able to send an email with attachement */
"org.nuxeo.ecm.platform.url.api", "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.notification.core",
        "org.nuxeo.ecm.platform.notification.api", "org.nuxeo.ecm.platform.placeful.api" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
@Ignore
// wrong content and no smtp mock, not an unit test at all
public class TrialSendViaGmail {

    @Test
    public void test() throws Exception {
        String repository = this.getClass().getAnnotation(RepositoryConfig.class).repositoryName();

        // sender account
        final String defaultFrom = "nuxeomailtester@gmail.com"; //
        final String password = "nuxeo1mailtester2";
        // configureGmailSmtp(defaultFrom, password);
        configureIntranetSmtp(defaultFrom, password);

        // content to send
        File f = new File("src/test/resources/TestAclLayoutGenerated.xls");
        Blob fb = Blobs.createBlob(f, "application/xls");
        fb.setFilename(f.getName());

        // do send
        String to = defaultFrom;
        IResultPublisher p = new PublishByMail(to, defaultFrom, repository);
        p.publish(fb);
    }

    /**
     * Configure Mailer properties to access a gmail sender account.
     */
    public void configureGmailSmtp(final String defaultFrom, final String password) {
        Mailer m = SendMail.COMPOSER.getMailer();
        m.getConfiguration().put("mail.smtp.auth", "true");
        m.getConfiguration().put("mail.smtp.starttls.enable", "true");
        m.getConfiguration().put("mail.smtp.host", "smtp.gmail.com");
        m.getConfiguration().put("mail.smtp.port", "587");
        m.getConfiguration().put("mail.smtp.user", defaultFrom);
        m.getConfiguration().put("mail.smtp.password", password);
        m.setAuthenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(defaultFrom, password);
            }
        });
        // GMAIL Verification code: bd63ad13-98eb2234-8ffd777412
    }

    public void configureIntranetSmtp(final String defaultFrom, final String password) {
        Mailer m = SendMail.COMPOSER.getMailer();
        m.getConfiguration().put("mail.smtp.host", "smtp.in.nuxeo.com");
        m.getConfiguration().put("mail.smtp.user", defaultFrom);
        m.getConfiguration().put("mail.smtp.password", password);
        m.setAuthenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(defaultFrom, password);
            }
        });
        // GMAIL Verification code: bd63ad13-98eb2234-8ffd777412
    }

}
