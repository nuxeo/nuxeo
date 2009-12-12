package org.nuxeo.ecm.webdav.provider;

import org.nuxeo.ecm.webdav.Util;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
@Produces("application/xml")
public class WebDavContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext ctx;

    public WebDavContextResolver() throws JAXBException {
        ctx = Util.getJaxbContext();
    }

    public JAXBContext getContext(Class<?> type) {
        if (type.getPackage().getName().startsWith("net.java.dev.webdav.jaxrs.xml.elements")) {
            return ctx;
        } else {
            return null;
        }
    }

}
