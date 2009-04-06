package org.nuxeo.webengine.captcha;

import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
  import com.octo.captcha.service.image.ImageCaptchaService;
   
   public class CaptchaServiceSingleton {
   
       private static ImageCaptchaService instance = new DefaultManageableImageCaptchaService();
   
     public static ImageCaptchaService getInstance() {
          return instance;
      }
  
 }