/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.CssRewriter;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.RewriterResults;
import org.apache.shindig.gadgets.rewrite.RewriterUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Singleton
public class NXCSSContentRewriter implements ContentRewriter {

    private final ContentRewriterFeatureFactory rewriterFeatureFactory;

    private final String proxyBaseNoGadget;

    @Inject
    public NXCSSContentRewriter(
            ContentRewriterFeatureFactory rewriterFeatureFactory,
            @Named("shindig.content-rewrite.proxy-url") String proxyBaseNoGadget) {
        this.rewriterFeatureFactory = rewriterFeatureFactory;
        this.proxyBaseNoGadget = proxyBaseNoGadget;
    }

    public RewriterResults rewrite(Gadget gadget, MutableContent content) {
        // Not supported
        return null;
    }

    public RewriterResults rewrite(HttpRequest request, HttpResponse original,
            MutableContent content) {
        if (!RewriterUtils.isCss(request, original)) {
            return null;
        }
        ContentRewriterFeature feature = rewriterFeatureFactory.get(request);
        content.setContent(CssRewriter.rewrite(content.getContent(),
                request.getUri(), createLinkRewriter(request.getGadget(),
                        feature)));

        return RewriterResults.cacheableIndefinitely();
    }

    protected LinkRewriter createLinkRewriter(Uri gadgetUri,
            ContentRewriterFeature feature) {
        return new NXLinkRewriter(gadgetUri, feature, proxyBaseNoGadget);
    }

}
