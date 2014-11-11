package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterUris;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NXLinkRewriter extends ProxyingLinkRewriter {

    public NXLinkRewriter(ContentRewriterUris rewriterUris, Uri gadgetUri,
            ContentRewriterFeature rewriterFeature, String container,
            boolean debug, boolean ignoreCache) {
        super(rewriterUris, gadgetUri, rewriterFeature, container, debug,
                ignoreCache);
    }

    @Override
    public String rewrite(String link, Uri context) {
        link = link.replace("${org.nuxeo.ecm.contextPath}",
                VirtualHostHelper.getContextPathProperty());
        return super.rewrite(link, context);
    }

}
