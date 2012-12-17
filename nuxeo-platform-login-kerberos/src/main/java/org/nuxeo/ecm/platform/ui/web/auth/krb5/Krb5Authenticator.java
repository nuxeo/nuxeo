package org.nuxeo.ecm.platform.ui.web.auth.krb5;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
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
	
	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	private static final String AUTHORIZATION = "Authorization";
	private static final String NEGOTIATE = "Negotiate";
	private static final String SKIP_KERBEROS = "X-Skip-Kerberos"; // magic header used by the reverse proxy to skip this authenticator
	

	private static final GSSManager MANAGER = GSSManager.getInstance();
	private LoginContext loginContext = null;
	private GSSCredential serverCredential = null;
	private boolean disabled = false;

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
		String authorization = req.getHeader(AUTHORIZATION);
		if (authorization == null) {
			return null; // no auth
		}
		
		if (!authorization.startsWith(NEGOTIATE)) {
			logger.warn("Received invalid Authorization header (expected: Negotiate then SPNEGO blob): " + authorization);
			// ignore invalid authorization headers.
			return null;
		}
		
        byte[] token = new Base64(-1).decode(authorization.substring(NEGOTIATE.length() + 1));
        byte[] respToken = null;
        
        GSSContext context = null;
        
        try {
			synchronized (this) {
				context = MANAGER.createContext(serverCredential);
				respToken = context.acceptSecContext(token, 0, token.length);
				
			}
			if (context.isEstablished()) {
				String principal = context.getSrcName().toString();
				String username = principal.split("@")[0]; // throw away the realm
				UserIdentificationInfo info = new UserIdentificationInfo(username, "Trust");
				info.setLoginPluginName("Trusting_LM");
				return info;
			} else {
				// need another roundtrip
				res.setHeader(WWW_AUTHENTICATE, NEGOTIATE + " " + new Base64(-1).encode(respToken));
				return null;
			}
			
        } catch (GSSException ge) {
        	logger.error("Cannot accept provided security token", ge);
        	return null;
        }
		
	}

	@Override
	public void initPlugin(Map<String, String> parameters) {
		
		try {
			this.loginContext = new LoginContext("Nuxeo");
			// note: we assume that all configuration is done in loginconfig, so there are NO parameters here
			loginContext.login();
			serverCredential = Subject.doAs(loginContext.getSubject(), getServerCredential);
		} catch(LoginException le) {
			logger.error("Cannot create LoginContext, disabling Kerberos module", le);
			this.disabled = true;
		} catch(PrivilegedActionException pae) {
			logger.error("Cannot get server credentials, disabling Kerberos module", pae);
			this.disabled = true;
		} 
		
	}

	@Override
	public Boolean needLoginPrompt(HttpServletRequest req) {
		return !disabled && req.getHeader(SKIP_KERBEROS) == null;
	}

	
	private PrivilegedExceptionAction<GSSCredential> getServerCredential = new PrivilegedExceptionAction<GSSCredential>() {
		
		@Override
		public GSSCredential run() throws GSSException {
			return MANAGER.createCredential(null,
					GSSCredential.DEFAULT_LIFETIME, new Oid("1.3.6.1.5.5.2"), /* Oid for Kerberos */
					GSSCredential.ACCEPT_ONLY);
		}
	};
}
