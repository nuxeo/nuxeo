package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import java.util.Map;
import java.util.Set;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.lexer.LinkingTagRewriter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NXLinkingTagRewriter extends LinkingTagRewriter {

    public static Map<String, Set<String>> getDefaultTargets() {
        Map<String, Set<String>> targets = new ImmutableMap.Builder<String, Set<String>>().put(
                "img", ImmutableSet.of("src")).put("embed",
                ImmutableSet.of("src")).put("link", ImmutableSet.of("href"))
                .put("input", ImmutableSet.of("src")).build();
        return targets;
    }

    public NXLinkingTagRewriter(LinkRewriter linkRewriter, Uri relativeBase) {
        super(getDefaultTargets(), linkRewriter, relativeBase);
    }

}
