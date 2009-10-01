package org.nuxeo.ecm.platform.oauth.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.signature.OAuthSignatureMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.oauth.api.OAuthService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class OAuthServiceTest {

  private static final Random random = new Random();

  private OAuthService service;

  @Inject
  public OAuthServiceTest(TestRuntimeHarness harness) throws Exception {
    harness.deployContrib("org.nuxeo.ecm.platform.oauth", "OSGI-INF/oauth-service.xml");
    harness.deployContrib("org.nuxeo.ecm.platform.oauth.test",
        "oauth-service-contrib.xml");

    service = Framework.getService(OAuthService.class);
    assertTrue(service != null);
  }


  @Test
  public void getConsumer() {
    OAuthConsumer consumer = service.getOAuthConsumer("clientConsumerWithAPublicKey");
    assertTrue(consumer != null);
  }

  @Test
  public void consumerWithAPubliKeyCanVerifyAMessage() throws Exception {

    String urlStr = "http://www.google.com/foo?param1=value1&param2=value2";
    URL url = new URL(urlStr);




    OAuthConsumer serverConsumer = service.getOAuthConsumer("clientConsumerWithAPublicKey");
    assertTrue(serverConsumer != null);

    OAuthConsumer clientConsumer = service.getOAuthConsumer("consumerWithAPrivateKey");
    assertTrue(clientConsumer != null);


    OAuthMessage message = prepareRequestMessage(clientConsumer, "GET", url,
        OAuth.RSA_SHA1);
    OAuthAccessor accessor = new OAuthAccessor(clientConsumer);
    try {
      message.sign(accessor);
    } catch (Exception e) {
      fail("Failed to sign the message");
    }

    OAuthAccessor serverAccessor = new OAuthAccessor(serverConsumer);
    try {
      OAuthSignatureMethod.newSigner(message, serverAccessor)
          .validate(message);
    } catch (Exception e) {
      fail("Failed to verify the message");
    }

  }


  public void canVerifyAMessageWithItsPublicKeyTest() throws Exception {
    String urlStr = "http://www.google.com/foo?param1=value1&param2=value2";
    URL url = new URL(urlStr);

    OAuthConsumer clientConsumer = service.getOAuthConsumer("clientConsumerWithAPrivateKey");
    assertTrue(clientConsumer != null);

    OAuthAccessor clientAccessor = new OAuthAccessor(clientConsumer);
    OAuthMessage message = prepareRequestMessage(clientConsumer, "GET", url,
        OAuth.RSA_SHA1);

    try {
      message.sign(clientAccessor);
    } catch (Exception e) {
      fail("Failed to sign the message");
    }

    OAuthConsumer serverConsumer = service.getOAuthConsumer("serverConsumer");
    OAuthAccessor serverAccessor = new OAuthAccessor(serverConsumer);

    try {
      OAuthSignatureMethod.newSigner(message, serverAccessor)
          .validate(message);
    } catch (Exception e) {
      fail("Failed to verify the message");
    }

  }

  private static OAuthMessage prepareRequestMessage(OAuthConsumer consumer,
      String httpMethod, URL url, String signatureMethod) {
    OAuthMessage message = new OAuthMessage(httpMethod, url.toString(),
        new ArrayList<OAuth.Parameter>());
    message.addParameter(OAuth.OAUTH_SIGNATURE_METHOD, signatureMethod);
    message.addParameter(OAuth.OAUTH_VERSION, OAuth.VERSION_1_0);
    message.addParameter(OAuth.OAUTH_CONSUMER_KEY, consumer.consumerKey);
    long currentTime = System.currentTimeMillis() / 1000l;
    message.addParameter(OAuth.OAUTH_TIMESTAMP, Long.toString(currentTime, 10));
    byte[] nonce = new byte[8];
    random.nextBytes(nonce);
    BigInteger nonceInt = new BigInteger(1, nonce);
    message.addParameter(OAuth.OAUTH_NONCE, nonceInt.toString(10));
    return message;
  }
}
