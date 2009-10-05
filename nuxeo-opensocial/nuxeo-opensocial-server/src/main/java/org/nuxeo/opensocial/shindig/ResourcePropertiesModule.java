/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

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
