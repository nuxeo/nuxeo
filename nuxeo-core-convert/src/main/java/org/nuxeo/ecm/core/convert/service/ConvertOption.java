package org.nuxeo.ecm.core.convert.service;

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
