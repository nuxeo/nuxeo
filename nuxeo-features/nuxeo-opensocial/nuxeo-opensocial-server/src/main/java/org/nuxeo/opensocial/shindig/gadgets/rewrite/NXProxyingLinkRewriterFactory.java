/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

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
