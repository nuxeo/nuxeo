/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.service;

/**
 * Helper class to manage mime-types chains
 * @author tiry
 *
 */
public class ConvertOption {

      protected String mimeType;
      protected String converter;

      public ConvertOption(String converter, String mimeType) {
          this.mimeType=mimeType;
          this.converter = converter;
      }

      public String getMimeType() {
          return mimeType;
      }
      public String getConverterName() {
          return converter;
      }
}
