package org.nuxeo.opensocial.shindig.gadgets;

import java.util.concurrent.ExecutorService;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.AbstractSpecFactory;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.servlet.HttpGadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.google.inject.name.Named;

//I had to copy defaultgadgetspecfactory because it uses a call to super
//in getgagdetspec and thus I cannot override it properly (since I cannot
//call supersuper.getgadgetspec

public class NXGadgetSpecFactory extends AbstractSpecFactory<GadgetSpec>
        implements GadgetSpecFactory {
    public static final String CACHE_NAME = "gadgetSpecs";

    static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";

    static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");

    protected long refresh;

    protected RequestPipeline pipeline;

    @Inject
    public NXGadgetSpecFactory(ExecutorService executor,
            RequestPipeline pipeline, CacheProvider cacheProvider,
            @Named("shindig.cache.xml.refreshInterval") long refresh) {
        super(GadgetSpec.class, executor, pipeline, makeCache(cacheProvider),
                refresh);
        this.refresh = refresh;
        this.pipeline = pipeline;
    }

    private static Cache<Uri, Object> makeCache(CacheProvider cacheProvider) {
        return cacheProvider.createCache(CACHE_NAME);
    }

    public GadgetSpec getGadgetSpec(GadgetContext context)
            throws GadgetException {
        String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
        if (rawxml != null) {
            // Set URI to a fixed, safe value (localhost), preventing a gadget
            // rendered
            // via raw XML (eg. via POST) to be rendered on a locked domain of
            // any other
            // gadget whose spec is hosted non-locally.
            try {
                return new GadgetSpec(RAW_GADGET_URI, XmlUtil.parse(rawxml),
                        rawxml);
            } catch (XmlException e) {
                throw new SpecParserException(e);
            }
        }

        Uri gadgetUri = context.getUrl();
        if (context instanceof HttpGadgetContext) {
            HttpGadgetContext ctx = (HttpGadgetContext) context;
        }
        // if (context instanceof HttpGadgetContext) {
        // HttpGadgetContext ctx = (HttpGadgetContext) context;
        // try {
        // Field f = ctx.getClass().getField("request");
        // Object raw = f.get(ctx);
        // HttpServletRequest request = (HttpServletRequest) raw;
        // String val = request.getHeader("Cookie");
        //
        // } catch (SecurityException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (NoSuchFieldException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IllegalArgumentException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IllegalAccessException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }

        Query query = new MyQuery("jsessionid", "foo").setSpecUri(gadgetUri).setContainer(
                context.getContainer()).setGadgetUri(gadgetUri).setIgnoreCache(
                context.getIgnoreCache());
        return super.getSpec(query);
    }

    @Override
    protected GadgetSpec fetchFromNetwork(Query query) throws GadgetException {
        HttpRequest request = new HttpRequest(query.getSpecUri()).setIgnoreCache(
                query.getIgnoreCache()).setGadget(query.getGadgetUri()).setContainer(
                query.getContainer());

        if (query instanceof MyQuery) {
            MyQuery q = (MyQuery) query;
            request.addHeader(q.getName(), q.getValue());
        }

        // Since we don't allow any variance in cache time, we should just force
        // the cache time
        // globally. This ensures propagation to shared caches when this is set.
        request.setCacheTtl((int) (refresh / 1000));

        HttpResponse response = pipeline.execute(request);
        if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
            throw new GadgetException(
                    GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                    "Unable to retrieve spec for " + query.getSpecUri()
                            + ". HTTP error " + response.getHttpStatusCode());
        }

        try {
            String content = response.getResponseAsString();
            return parse(content, query);
        } catch (XmlException e) {
            throw new SpecParserException(e);
        }
    }

    @Override
    protected GadgetSpec parse(String content, Query query)
            throws XmlException, GadgetException {
        Element element = XmlUtil.parse(content);
        return new GadgetSpec(query.getSpecUri(), element, content);
    }

    /*
     * This is horrible hack to deal with the fact that the constructor has
     * weird visibility on Query
     */
    public static class MyQuery extends AbstractSpecFactory.Query {
        protected String key;

        protected String value;

        public MyQuery(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getName() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
