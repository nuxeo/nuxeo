package org.nuxeo.opensocial.service.test;

import static org.easymock.EasyMock.expect;

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.servlet.ServletTestFixture;
import org.nuxeo.opensocial.shindig.gadgets.NXMakeRequestHandler;

public class NXMakeRequestHandlerTest extends ServletTestFixture {

  private static final FakeGadgetToken DUMMY_TOKEN = new FakeGadgetToken();

  public void testObtenirRequeteSignee() {
    expect(request.getParameter(NXMakeRequestHandler.AUTHZ_PARAM)).andReturn(
        AuthType.SIGNED.toString());
    replay();

    assertEquals(request.getParameter(NXMakeRequestHandler.AUTHZ_PARAM)
        .toString(), AuthType.SIGNED.toString());
  }

  public void testObtenirRequeteAvecToken() {
    expect(request.getParameter(NXMakeRequestHandler.AUTHZ_PARAM)).andReturn(
        AuthType.SIGNED.toString());
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId())).andReturn(
        DUMMY_TOKEN)
        .atLeastOnce();
    replay();

    assertEquals(
        request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId()),
        DUMMY_TOKEN);
  }

}
