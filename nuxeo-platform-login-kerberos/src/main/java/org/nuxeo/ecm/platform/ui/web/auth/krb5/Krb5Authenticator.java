package org.nuxeo.ecm.platform.ui.web.auth.krb5;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jcifs.Config;
import jcifs.spnego.Authentication;
import jcifs.spnego.AuthenticationException;
import jcifs.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

/**
 * Kerberos v5 in SPNEGO authentication.
 * 
 * TODO handle NTLMSSP as a fallback position.
 * 
 * @author schambon
 *
 */
public class Krb5Authenticator implements NuxeoAuthenticationPlugin {
	
	private static final Log logger = LogFactory.getLog(Krb5Authenticator.class);
	
	private static final String JAVA_LOGIN_CONFIG = "javaLoginConfig";
	private static final String REALM = "realm";
	private static final String KDC = "kdc";
	private static final String SERVICE_PRINCIPAL_NAME = "servicePrincipalName";
	
	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	private static final String AUTHORIZATION = "Authorization";
	private static final String NEGOTIATE = "Negotiate";
	
	private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
	private static final String JAVA_SECURITY_KRB5_REALM = "java.security.krb5.realm";
	private static final String JAVA_SECURITY_KRB5_KDC = "java.security.krb5.kdc";
	private static final String JCIFS_SPNEGO_SERVICEPRINCIPAL = "jcifs.spnego.servicePrincipal";

	private Authentication auth = new Authentication();

	@Override
	public List<String> getUnAuthenticatedURLPrefix() {
		return null;
	}

	@Override
	public Boolean handleLoginPrompt(HttpServletRequest req,
		HttpServletResponse res, String baseURL) {
		
		logger.debug("Sending login prompt...");
		res.setHeader(WWW_AUTHENTICATE, NEGOTIATE);
		res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		res.setContentLength(0);
		try {
			res.flushBuffer();
			
		} catch (IOException e) {
			logger.warn("Cannot flush response", e);
		}
		return true;
	}

	@Override
	public UserIdentificationInfo handleRetrieveIdentity(
			HttpServletRequest req, HttpServletResponse res) {
		String authorization = req.getHeader("Authorization");
		if (authorization == null) {
			return null; // no auth
		}
		// TODO traiter les cas d'erreur ex: quand le header ne commence pas par Negotiate
        byte[] token = Base64.decode(authorization.substring(NEGOTIATE.length() + 1));
        
		synchronized (this) {
			auth.reset();
			
			try {
				auth.process(token);
				Principal principal = auth.getPrincipal();
				String username = principal.getName().split("@")[0];
				return new UserIdentificationInfo(username, null);
			} catch (AuthenticationException e) {
				logger.error("Cannot authenticate", e);
			}
		}
		return null;
	}

	@Override
	public void initPlugin(Map<String, String> parameters) {

		System.setProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG, parameters.get(JAVA_LOGIN_CONFIG)); // for some reason this doesn't work?
		System.setProperty(JAVA_SECURITY_KRB5_REALM, parameters.get(REALM));
		System.setProperty(JAVA_SECURITY_KRB5_KDC, parameters.get(KDC));
		Config.setProperty(JCIFS_SPNEGO_SERVICEPRINCIPAL, parameters.get(SERVICE_PRINCIPAL_NAME));
	}

	@Override
	public Boolean needLoginPrompt(HttpServletRequest req) {
		return true;
	}

}
