package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriterFactory;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;
import org.nuxeo.opensocial.shindig.gadgets.rewrite.NXProxyingLinkRewriterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a> */
public class NXGadgetModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(MakeRequestHandler.class).to(NXMakeRequestHandler.class)
        .in(Scopes.SINGLETON);
    bind(ProxyingLinkRewriterFactory.class).to(
        NXProxyingLinkRewriterFactory.class)
        .in(Scopes.SINGLETON);
  }

}
