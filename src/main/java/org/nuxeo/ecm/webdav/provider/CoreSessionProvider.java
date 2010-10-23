package org.nuxeo.ecm.webdav.provider;

import com.sun.jersey.api.core.HttpContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;

import javax.ws.rs.Consumes;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Type;

@Provider
public class CoreSessionProvider extends AbstractInjectableProvider<CoreSession> {

    public CoreSessionProvider() {
        super(CoreSession.class);
    }

    @Override
    public CoreSession getValue(HttpContext c) {
        try {
            Util.startTransaction();
            return Util.getSession();
        } catch (Exception e) {
            return null;
        }
    }

}
