package org.nuxeo.template.xdocreport.jaxrs;

import java.io.OutputStream;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public class JSONHelper {

    public static void writeResource(Resource resource, OutputStream out)
            throws Exception {
        String prefix = "{ \"resource\" : { \"name\" : \"resources\", \"type\" : \"CATEGORY\", \"children\" : ";
        String suffix = " }}";
        out.write(prefix.getBytes());
        JsonGenerator gen = JsonHelper.createJsonGenerator(out);
        gen.writeObject(resource);
        out.write(suffix.getBytes());
    }

}
