package org.nuxeo.scim.server.jaxrs.marshalling;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.nuxeo.common.utils.FileUtils;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Copy of the original scimsdk class just to change some org.json constructors
 *
 * scimsdk uses a custom version of org.json with a different artifactId and
 * some code differences but with the same namespace
 *
 * @author tiry
 * @since 7.4
 *
 */
public class NXJsonUnmarshaller extends JsonUnmarshaller {

    @Override
    public <R extends BaseResource> R unmarshal(final InputStream inputStream,
            final ResourceDescriptor resourceDescriptor,
            final ResourceFactory<R> resourceFactory)
            throws InvalidResourceException {
        try {

            String json = FileUtils.read(inputStream);
            final JSONObject jsonObject = makeCaseInsensitive(new JSONObject(
                    new JSONTokener(json)));

            final NXJsonParser parser = new NXJsonParser();
            return parser.doUnmarshal(jsonObject, resourceDescriptor,
                    resourceFactory, null);
        } catch (JSONException e) {
            throw new InvalidResourceException("Error while reading JSON: "
                    + e.getMessage(), e);
        } catch (IOException e) {
            throw new InvalidResourceException("Error while reading JSON: "
                    + e.getMessage(), e);
        }
    }

    protected JSONObject makeCaseInsensitive(final JSONObject jsonObject)
            throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        Iterator keys = jsonObject.keys();
        Map lowerCaseMap = new HashMap(jsonObject.length());
        while (keys.hasNext()) {
            String key = keys.next().toString();
            String lowerCaseKey = toLowerCase(key);
            lowerCaseMap.put(lowerCaseKey, jsonObject.get(key));
        }

        return new JSONObject(lowerCaseMap);
    }
}
