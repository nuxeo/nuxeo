package org.nuxeo.opensocial.shindig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.util.ResourceLoader;

import com.google.inject.CreationException;
import com.google.inject.spi.Message;

public class ResourcePropertiesModule extends PropertiesModule {

  public ResourcePropertiesModule(String propertiesPath) {
    super(loadPropertiesFrom(propertiesPath));
  }

  public static Properties loadPropertiesFrom(String propertyPath) {
    InputStream is = null;
    try {
      is = ResourceLoader.openResource(propertyPath);
      Properties properties = new Properties();
      properties.load(is);
      return properties;
    } catch (IOException e) {
      throw new CreationException(Arrays.asList(new Message(
          "Unable to load properties: " + propertyPath)));
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException e) {
        // weird
      }
    }
  }
}
