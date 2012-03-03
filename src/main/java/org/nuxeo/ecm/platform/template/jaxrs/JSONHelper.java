package org.nuxeo.ecm.platform.template.jaxrs;

import java.io.OutputStream;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

public class JSONHelper {

    public static void writeResource(Resource resource, OutputStream out) throws Exception {
        String prefix = "{ \"resource\" : { \"name\" : \"resources\", \"type\" : 0, \"children\" : ";
        String suffix = " }}";
        out.write(prefix.getBytes());
        JsonGenerator gen = JsonWriter.createGenerator(out);        
        gen.writeObject(resource);
        out.write(suffix.getBytes());
    }
    
}
