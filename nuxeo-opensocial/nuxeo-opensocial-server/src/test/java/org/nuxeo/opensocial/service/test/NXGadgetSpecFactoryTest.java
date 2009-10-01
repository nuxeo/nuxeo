package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.opensocial.shindig.gadgets.NXGadgetSpecFactoryModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class NXGadgetSpecFactoryTest {

  public static final Uri SPEC_URL = Uri.parse("http://www.example.org/dir/g.xml");
  public static final Uri SPEC_URL_TO_SIGN = Uri.parse("http://www.example.org/dir/g.xml@@getDef");
  private GadgetSpecFactory factory;

  public static GadgetSpec createSpecWithoutRewrite() throws GadgetException {
    String xml = "<Module>" + "<ModulePrefs title=\"title\">"
        + "</ModulePrefs>" + "<Content type=\"html\">Hello!</Content>"
        + "</Module>";
    return new GadgetSpec(SPEC_URL, xml);
  }

  @BeforeClass
  public static void setUpOnce() throws GadgetException {
    String xml = "<Module>" + "<ModulePrefs title=\"title\">"
        + "</ModulePrefs>" + "<Content type=\"html\">Hello!</Content>"
        + "</Module>";
    RecordingFetcher.setReponse(new HttpResponseBuilder().setHttpStatusCode(200)
        .setResponseString(xml)
        .setHeader("Content-type", "application/json")
        .create());
  }

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new GadgetSpecFactoryTestModule(),
        new NXGadgetSpecFactoryModule(), new DefaultGuiceModule(), new PropertiesModule(), new OAuthModule());

    factory = injector.getInstance(GadgetSpecFactory.class);
    assertNotNull(factory);

  }

  public void unFetchSurUneDefinitionDansNuxeoEstSigne() throws GadgetException {
    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL_TO_SIGN.toJavaUri();
      }

      @Override
      public SecurityToken getToken() {
        return new FakeGadgetToken();
      }
    };

    factory.getGadgetSpec(context);

    HttpRequest request = RecordingFetcher.getLastRequest();
    assertEquals(AuthType.SIGNED, request.getAuthType());

  }

  @Test
  public void unFetchSurUneDefinitionNormalNEstPasSigne()
      throws GadgetException {
    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }

      @Override
      public SecurityToken getToken() {
        return new FakeGadgetToken();
      }
    };

    factory.getGadgetSpec(context);

    HttpRequest request = RecordingFetcher.getLastRequest();
    assertEquals(AuthType.NONE, request.getAuthType());

  }

  @Test
  public void unFetchSurUneSimpleURINeRenvoitPasNull() throws GadgetException,
      URISyntaxException {
    GadgetSpec spec = factory.getGadgetSpec(new URI("http://google.fr/a.xml"),
        false);
    assertNotNull(spec);
  }
}
