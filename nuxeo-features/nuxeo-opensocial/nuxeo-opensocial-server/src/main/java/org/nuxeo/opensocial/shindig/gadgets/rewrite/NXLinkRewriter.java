package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NXLinkRewriter extends ProxyingLinkRewriter {

    public NXLinkRewriter(Uri gadgetUri,
            ContentRewriterFeature rewriterFeature, String prefix) {
        super(gadgetUri, rewriterFeature, prefix);
    }

    @Override
    public String rewrite(String link, Uri context) {
        link = link.replace("${org.nuxeo.ecm.contextPath}",
                VirtualHostHelper.getContextPathProperty());
        return super.rewrite(link, context);
    }

}
