package org.nuxeo.webengine.sites.wiki.rendering;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.rendering.wiki.WikiFilter;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializer;
import org.nuxeo.ecm.platform.rendering.wiki.WikiTransformer;
import org.nuxeo.ecm.platform.rendering.wiki.extensions.PatternFilter;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
@XObject("rendering-extension")
public class WikiSitesTransformerDescriptor extends
        RenderingExtensionDescriptor {

    @XNode("@name")
    protected void setName(String name) {
        this.name = name;
    }

    @XNode("@class")
    protected void setClassName(Class<?> klass) {
        this.klass = klass;
    }

    @XNode("@serializer")
    protected Class<?> serializerClass;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = WikiSitesFilterDescriptor.class)
    protected List<WikiSitesFilterDescriptor> filters;

    @Override
    public WikiTransformer newInstance() throws Exception {
        WikiTransformer tr;
        if (serializerClass == null) {
            tr = new WikiTransformer();
        } else {
            tr = new WikiTransformer(
                    (WikiSerializer) serializerClass.newInstance());
        }
        WikiSerializer serializer = tr.getSerializer();
        for (WikiSitesFilterDescriptor wfd : filters) {
            if (wfd.clazz != null) {
                Class<?> clazz = Class.forName(wfd.clazz);
                WikiFilter filter = (WikiFilter) clazz.newInstance();
                serializer.addFilter(filter);
            } else {
                serializer.addFilter(new PatternFilter(wfd.pattern,
                        wfd.replacement));
            }
        }
        return tr;
    }

}
