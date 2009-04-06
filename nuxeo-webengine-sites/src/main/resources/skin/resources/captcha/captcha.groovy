 
 	// get the id from the querystring and display a captcha image
	import java.io.ByteArrayOutputStream;
 	import org.nuxeo.webengine.captcha.CaptchaServiceSingleton;
 	import com.sun.image.codec.jpeg.JPEGCodec;
 	import java.util.Locale;
 	//waiting for a querystring
	//def captcha = Context.getRequest().getQueryString();
 	def captcha = Context.getRequest().getParameter("captcha");
 	def lang = Context.getRequest().getParameter("lang");
 	def response = Context.getResponse()
 	
 	
 	def captchaChallengeAsJpeg = null;
 	// the output stream to render the captcha image as jpeg into
 	def jpegOutputStream = new ByteArrayOutputStream();
 	try {
 	  def challenge = null;
 	  System.out.println(" generating captcha "+lang);
 	  if(lang != null){
 	    challenge = CaptchaServiceSingleton.getInstance().getImageChallengeForID(captcha,  new Locale(lang));
 	  }
 	  else {
 	    challenge = CaptchaServiceSingleton.getInstance().getImageChallengeForID(captcha);
 	  }
 	  // a jpeg encoder
 	  def jpegEncoder = JPEGCodec.createJPEGEncoder(jpegOutputStream);
 	  jpegEncoder.encode(challenge);
 	} catch (Exception e) {
 	 return;
 	}
	
 	
 	captchaChallengeAsJpeg = jpegOutputStream.toByteArray();
	
 	// flush it in the response
 	response.setHeader("Cache-Control", "no-store");
 	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);
 	response.setContentType("image/jpeg");
 	def os = response.getOutputStream();
 	os.write(captchaChallengeAsJpeg);
 	os.flush();