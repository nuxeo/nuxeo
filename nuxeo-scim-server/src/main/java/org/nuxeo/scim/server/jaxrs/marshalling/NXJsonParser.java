package org.nuxeo.scim.server.jaxrs.marshalling;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.json.JsonParser;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Hack to make a method public !
 *
 * @author tiry
 * @since 7.4
 */
public class NXJsonParser extends JsonParser {

    public <R extends BaseResource> R doUnmarshal(
            final JSONObject jsonObject,
            final ResourceDescriptor resourceDescriptor,
            final ResourceFactory<R> resourceFactory,
            final JSONArray defaultSchemas)
            throws JSONException, InvalidResourceException {
        return super.unmarshal(jsonObject, resourceDescriptor, resourceFactory, defaultSchemas);
    }

}
