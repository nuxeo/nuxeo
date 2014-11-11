package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.DomUtil;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ConcatLinkRewriterFactory;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeatureFactory;
import org.apache.shindig.gadgets.rewrite.CssRequestRewriter;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.LinkRewriter;
import org.apache.shindig.gadgets.rewrite.MutableContent;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriterFactory;
import org.apache.shindig.gadgets.rewrite.RequestRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterUtils;
import org.apache.shindig.gadgets.spec.View;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * This class extends the vanilla HTMLContentRewriter by adding the INPUT tag to
 * replace its src property
 * 
 * @author dmetzler
 * 
 */
public class NXHTMLContentRewriter implements GadgetRewriter, RequestRewriter {

    private final static String JS_MIME_TYPE = "text/javascript";

    public final static Set<String> TAGS = ImmutableSet.of("img", "embed",
            "link", "script", "style", "input");

    private final static ImmutableMap<String, ImmutableSet<String>> LINKING_TAG_ATTRS = ImmutableMap.of(
            "img", ImmutableSet.of("src"), "embed", ImmutableSet.of("src"),
            "input", ImmutableSet.of("src"));

    private final ContentRewriterFeatureFactory rewriterFeatureFactory;

    private final CssRequestRewriter cssRewriter;

    private final ConcatLinkRewriterFactory concatLinkRewriterFactory;

    private final ProxyingLinkRewriterFactory proxyingLinkRewriterFactory;

    @Inject
    public NXHTMLContentRewriter(
            ContentRewriterFeatureFactory rewriterFeatureFactory,
            CssRequestRewriter cssRewriter,
            ConcatLinkRewriterFactory concatLinkRewriterFactory,
            ProxyingLinkRewriterFactory proxyingLinkRewriterFactory) {
        this.rewriterFeatureFactory = rewriterFeatureFactory;
        this.cssRewriter = cssRewriter;
        this.concatLinkRewriterFactory = concatLinkRewriterFactory;
        this.proxyingLinkRewriterFactory = proxyingLinkRewriterFactory;
    }

    public boolean rewrite(HttpRequest request, HttpResponse original,
            MutableContent content) {
        if (RewriterUtils.isHtml(request, original)) {
            ContentRewriterFeature feature = rewriterFeatureFactory.get(request);
            return rewriteImpl(feature, request.getGadget(), request.getUri(),
                    content, request.getContainer(), false,
                    request.getIgnoreCache());
        }

        return false;
    }

    public void rewrite(Gadget gadget, MutableContent content) {
        // Don't rewrite urls if caja is enabled since caja will inline them
        // anyway
        if (gadget.getSpec().getModulePrefs().getFeatures().containsKey("caja")
                || "1".equals(gadget.getContext().getParameter("caja"))) {
            return;
        }

        ContentRewriterFeature feature = rewriterFeatureFactory.get(gadget.getSpec());
        Uri contentBase = gadget.getSpec().getUrl();
        View view = gadget.getCurrentView();
        if (view != null && view.getHref() != null) {
            contentBase = view.getHref();
        }

        rewriteImpl(feature, gadget.getSpec().getUrl(), contentBase, content,
                gadget.getContext().getContainer(),
                gadget.getContext().getDebug(),
                gadget.getContext().getIgnoreCache());
    }

    boolean rewriteImpl(ContentRewriterFeature feature, Uri gadgetUri,
            Uri contentBase, MutableContent content, String container,
            boolean debug, boolean ignoreCache) {
        if (!feature.isRewriteEnabled() || content.getDocument() == null) {
            return false;
        }

        // Get ALL interesting tags
        List<Element> tagList = DomUtil.getElementsByTagNameCaseInsensitive(
                content.getDocument(), TAGS);

        Element head = (Element) DomUtil.getFirstNamedChildNode(
                content.getDocument().getDocumentElement(), "head");

        boolean mutated = false;

        // 1st step. Rewrite links in all embedded style tags. Convert @import
        // statements into
        // links and add them to the tag list.
        // Move all style and link tags into head and concat the link tags
        mutated = rewriteStyleTags(head, tagList, feature, gadgetUri,
                contentBase, container, debug, ignoreCache);
        // Concat script links
        mutated |= rewriteJsTags(tagList, feature, gadgetUri, contentBase,
                container, debug, ignoreCache);
        // Rewrite links in images, embeds etc
        mutated |= rewriteContentReferences(tagList, feature, gadgetUri,
                contentBase, container, debug, ignoreCache);

        if (mutated) {
            MutableContent.notifyEdit(content.getDocument());
        }

        return mutated;
    }

    protected boolean rewriteStyleTags(Element head, List<Element> elementList,
            ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase,
            String container, boolean debug, boolean ignoreCache) {
        if (!feature.getIncludedTags().contains("style")) {
            return false;
        }
        boolean mutated = false;

        // Filter to just style tags
        Iterable<Element> styleTags = Lists.newArrayList(Iterables.filter(
                elementList, new Predicate<Element>() {
                    public boolean apply(Element element) {
                        return element.getNodeName().equalsIgnoreCase("style");
                    }
                }));

        LinkRewriter linkRewriter = proxyingLinkRewriterFactory.create(
                gadgetUri, feature, container, debug, ignoreCache);

        for (Element styleTag : styleTags) {
            mutated |= true;
            if (styleTag.getParentNode() != head) {
                styleTag.getParentNode().removeChild(styleTag);
                head.appendChild(styleTag);
            }

            List<String> extractedUrls = cssRewriter.rewrite(styleTag,
                    contentBase, linkRewriter, true);
            for (String extractedUrl : extractedUrls) {
                // Add extracted urls as link elements to head
                Element newLink = head.getOwnerDocument().createElement("link");
                newLink.setAttribute("rel", "stylesheet");
                newLink.setAttribute("type", "text/css");
                newLink.setAttribute("href", extractedUrl);
                head.appendChild(newLink);
                elementList.add(newLink);
            }
        }

        // Filter to just stylesheet link tags
        List<Element> linkTags = Lists.newArrayList(Iterables.filter(
                elementList, new Predicate<Element>() {
                    public boolean apply(Element element) {
                        return element.getNodeName().equalsIgnoreCase("link")
                                && ("stylesheet".equalsIgnoreCase(element.getAttribute("rel")) || element.getAttribute(
                                        "type").toLowerCase().contains("css"));
                    }
                }));

        concatenateTags(feature, linkTags, gadgetUri, contentBase, "text/css",
                "href", container, debug, ignoreCache);

        return mutated;
    }

    protected boolean rewriteJsTags(List<Element> elementList,
            ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase,
            String container, boolean debug, boolean ignoreCache) {
        if (!feature.getIncludedTags().contains("script")) {
            return false;
        }
        boolean mutated = false;

        // Filter to just script tags
        List<Element> scriptTags = Lists.newArrayList(Iterables.filter(
                elementList, new Predicate<Element>() {
                    public boolean apply(Element node) {
                        if (node.getNodeName().equalsIgnoreCase("script")) {
                            String type = node.getAttribute("type");
                            return type == null || type.length() == 0
                                    || type.equalsIgnoreCase(JS_MIME_TYPE);
                        }
                        return false;
                    }
                }));

        List<Element> concatenateable = Lists.newArrayList();
        for (int i = 0; i < scriptTags.size(); i++) {
            Element scriptTag = scriptTags.get(i);
            Element nextSciptTag = null;
            if (i + 1 < scriptTags.size()) {
                nextSciptTag = scriptTags.get(i + 1);
            }
            if (scriptTag.hasAttribute("src")
                    && feature.shouldRewriteURL(scriptTag.getAttribute("src"))) {
                mutated = true;
                concatenateable.add(scriptTag);
                if (nextSciptTag == null
                        || !nextSciptTag.equals(getNextSiblingElement(scriptTag))) {
                    // Next tag is not concatenateable
                    concatenateTags(feature, concatenateable, gadgetUri,
                            contentBase, JS_MIME_TYPE, "src", container, debug,
                            ignoreCache);
                    concatenateable.clear();
                }
            } else {
                concatenateTags(feature, concatenateable, gadgetUri,
                        contentBase, JS_MIME_TYPE, "src", container, debug,
                        ignoreCache);
                concatenateable.clear();
            }
        }
        concatenateTags(feature, concatenateable, gadgetUri, contentBase,
                JS_MIME_TYPE, "src", container, debug, ignoreCache);
        return mutated;
    }

    protected boolean rewriteContentReferences(List<Element> elementList,
            ContentRewriterFeature feature, Uri gadgetUri, Uri contentBase,
            String container, boolean debug, boolean ignoreCache) {
        boolean mutated = false;
        LinkRewriter rewriter = proxyingLinkRewriterFactory.create(gadgetUri,
                feature, container, debug, ignoreCache);

        final Set<String> tagNames = Sets.intersection(
                LINKING_TAG_ATTRS.keySet(), feature.getIncludedTags());

        // Filter to just style tags
        Iterable<Element> tags = Iterables.filter(elementList,
                new Predicate<Element>() {
                    public boolean apply(Element node) {
                        return tagNames.contains(node.getNodeName().toLowerCase());
                    }
                });

        for (Element node : tags) {
            NamedNodeMap attributes = node.getAttributes();
            Set<String> rewriteable = LINKING_TAG_ATTRS.get(node.getNodeName().toLowerCase());
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                if (rewriteable.contains(attr.getNodeName().toLowerCase())) {
                    mutated = true;
                    attr.setNodeValue(rewriter.rewrite(attr.getNodeValue(),
                            contentBase));
                }
            }
        }
        return mutated;
    }

    private void concatenateTags(final ContentRewriterFeature feature,
            List<Element> tags, Uri gadgetUri, Uri contentBase,
            String mimeType, final String attr, String container,
            boolean debug, boolean ignoreCache) {
        // Filter out excluded URLs
        tags = Lists.newArrayList(Iterables.filter(tags,
                new Predicate<Element>() {
                    public boolean apply(Element element) {
                        return (element.hasAttribute(attr) && feature.shouldRewriteURL(element.getAttribute(attr)));
                    }
                }));

        // Eliminate duplicates while maintaining order
        LinkedHashSet<Uri> nodeRefList = Sets.newLinkedHashSet();
        for (Element tag : tags) {
            try {
                nodeRefList.add(contentBase.resolve(Uri.parse(tag.getAttribute(attr))));
            } catch (IllegalArgumentException e) {
                // Same behavior as JavascriptTagMerger
                // Perhaps switch to ignoring script src instead?
                throw new RuntimeException(e);
            }
        }

        List<Uri> concatented = concatLinkRewriterFactory.create(gadgetUri,
                feature, container, debug, ignoreCache).rewrite(mimeType,
                nodeRefList);

        for (int i = 0; i < tags.size(); i++) {
            if (i < concatented.size()) {
                // Set new URLs into existing tags
                tags.get(i).setAttribute(attr, concatented.get(i).toString());
            } else {
                // Remove remainder
                tags.get(i).getParentNode().removeChild(tags.get(i));
            }
        }
    }

    private Element getNextSiblingElement(Element elem) {
        Node n = elem;
        n = n.getNextSibling();
        while (n != null && n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
        }
        return (Element) n;
    }
}