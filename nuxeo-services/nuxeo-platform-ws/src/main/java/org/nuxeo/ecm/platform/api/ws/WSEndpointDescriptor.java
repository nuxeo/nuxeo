package org.nuxeo.ecm.platform.api.ws;

import static javax.xml.ws.Endpoint.WSDL_PORT;
import static javax.xml.ws.Endpoint.WSDL_SERVICE;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPBinding;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ws.WSEndpointManagerImpl;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
@XObject("endpoint")
public class WSEndpointDescriptor {
    @XNode("@name")
    public String name;

    @XNode("@address")
    public String address;

    @XNode("@implementor")
    public Class<?> clazz;

    @XNodeList(value = "handlers/handler", type = String[].class, componentType = Class.class)
    public Class<? extends Handler>[] handlers;

    @XNode("@namespace")
    public String namespace;

    @XNode("@wsdl")
    public String wsdl;

    @XNode("@port")
    public String port;

    @XNode("@service")
    public String service;

    @XNode("enable-mtom")
    public boolean mtom;

    public Object getImplementorInstance() throws IllegalAccessException,
            InstantiationException {
        return clazz != null ? clazz.newInstance() : null;
    }

    public Endpoint toEndpoint() throws IOException, IllegalAccessException, InstantiationException {
        Endpoint ep = Endpoint.create(getImplementorInstance());
        List<Source> metadata = new ArrayList<>();
        Map<String, Object> properties = new HashMap<>();

        if (!isBlank(port)) {
            properties.put(WSDL_PORT, new QName(namespace, port));
        }
        if (!isBlank(port)) {
            properties.put(WSDL_SERVICE, new QName(namespace, service));
        }

        if (!isBlank(wsdl)) {
            URL wsdlURL = WSEndpointManagerImpl.class.getClassLoader().getResource(wsdl);
            if (wsdlURL == null) {
                throw new FileNotFoundException("WSDL: " + wsdl);
            }
            Source src = new StreamSource(wsdlURL.openStream());
            src.setSystemId(wsdlURL.toExternalForm());
            metadata.add(src);
        }

        ep.setMetadata(metadata);
        ep.setProperties(properties);
        return ep;
    }

    public void configurePostPublishing(Endpoint ep) throws IllegalAccessException, InstantiationException {
        if (handlers != null) {
            List<Handler> handlerChain = ep.getBinding().getHandlerChain();
            for(Class<? extends Handler> handler : handlers) {
                handlerChain.add(handler.newInstance());
            }
            ep.getBinding().setHandlerChain(handlerChain);
        }

        if (mtom && ep.getBinding() instanceof SOAPBinding) {
            SOAPBinding binding = (SOAPBinding) ep.getBinding();
            binding.setMTOMEnabled(mtom);
        }
    }
}
