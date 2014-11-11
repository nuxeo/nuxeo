package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterUris;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriterFactory;

import com.google.inject.Inject;

public class NXProxyingLinkRewriterFactory implements
        ProxyingLinkRewriterFactory {

    private final ContentRewriterUris rewriterUris;

    @Inject
    public NXProxyingLinkRewriterFactory(ContentRewriterUris rewriterUris) {
        this.rewriterUris = rewriterUris;
    }

    public ProxyingLinkRewriter create(Uri gadgetUri,
            ContentRewriterFeature rewriterFeature, String container,
            boolean debug, boolean ignoreCache) {
        return new NXLinkRewriter(rewriterUris, gadgetUri, rewriterFeature,
                container, debug, ignoreCache);
    }
}
