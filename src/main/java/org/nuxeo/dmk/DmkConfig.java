package org.nuxeo.dmk;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("config")
public class DmkConfig {

	@XNode("@type") public String type = "html";
		
	@XNode("@port") public int port = 8081;
	
	@XObject("authinfo")
	public static class AuthInfo {
		
		@XNode("@user") public String user;
		
		@XNode("@password") public String password;
		
	}
	
	@XNode("authinfo") protected AuthInfo authinfo;
	
}
