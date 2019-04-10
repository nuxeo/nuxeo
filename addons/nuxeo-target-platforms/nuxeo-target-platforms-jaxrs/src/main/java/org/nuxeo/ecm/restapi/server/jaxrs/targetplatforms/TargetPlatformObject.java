package org.nuxeo.ecm.restapi.server.jaxrs.targetplatforms;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.impl.TargetPlatformFilterImpl;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.io.JSONExporter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@WebObject(type = "target-platforms")
@Produces(MediaType.APPLICATION_JSON)
public class TargetPlatformObject extends DefaultObject {
    private static final String PUBLIC_TP_CACHE_KEY = "PUBLIC_TP";

    private static final LoadingCache<String, String> PUBLIC_CACHE = CacheBuilder //
            .newBuilder() //
            .expireAfterAccess(5, TimeUnit.MINUTES) //
            .refreshAfterWrite(10, TimeUnit.MINUTES) //
            .recordStats() //
            .maximumSize(5).build(new CacheLoader<String, String>() {
                @Override
                public String load(String key) throws Exception {
                    return key;
                }
            });

    @GET
    public Object doGet() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("public")
    public Object doGetPublic() throws Exception {
        String platforms = PUBLIC_CACHE.get(PUBLIC_TP_CACHE_KEY, () -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                TargetPlatformService tps = Framework.getService(TargetPlatformService.class);
                List<TargetPlatform> res = tps.getAvailableTargetPlatforms(
                        new TargetPlatformFilterImpl(false, true, true, false, null));
                if (res == null) {
                    res = new ArrayList<>();
                }
                JSONExporter.exportToJson(res, baos, false);
                return new String(baos.toByteArray());
            }
        });

        return Response.status(Response.Status.OK).entity(platforms).build();
    }
}
