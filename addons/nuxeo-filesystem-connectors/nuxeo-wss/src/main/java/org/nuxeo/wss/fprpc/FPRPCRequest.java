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
 *     Thierry Delprat
 */
package org.nuxeo.wss.fprpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.fprpc.exceptions.MalformedFPRPCRequest;
import org.nuxeo.wss.servlet.WSSRequest;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Wraps {@link HttpServletRequest} to provide FP-RPC specific parsing.
 *
 * @author Thierry Delprat
 */
public class FPRPCRequest extends WSSRequest {

    private String windowsEncoding = System.getProperty(WSSConfig.DEFAULT_ENCODING);

    public static final int FPRPC_GET_REQUEST = 0;

    public static final int FPRPC_POST_REQUEST = 1;

    public static final int FPRPC_CAML_REQUEST = 2;

    protected String version;

    protected List<FPRPCCall> calls;

    protected int requestMode = FPRPC_GET_REQUEST;

    protected InputStream vermeerBinary = null;

    protected Principal principal;

    public FPRPCRequest(HttpServletRequest httpRequest, String sitePath) throws MalformedFPRPCRequest {
        super(httpRequest, sitePath);
        parseRequest();
    }

    protected void parseRequest() throws MalformedFPRPCRequest {

        principal = httpRequest.getUserPrincipal();
        // get Method
        if ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod())) {
            requestMode = FPRPC_GET_REQUEST;
            parseGETRequest();
        } else if ("POST".equals(httpRequest.getMethod())) {
            String ct = httpRequest.getHeader(FPRPCConts.FP_CONTENT_TYPE_HEADER);
            if (FPRPCConts.FORM_ENCODED_CONTENT_TYPE.equals(ct)) {
                requestMode = FPRPC_POST_REQUEST;
                parsePOSTRequest();
            } else if (FPRPCConts.VERMEER_ENCODED_CONTENT_TYPE.equals(ct)) {
                requestMode = FPRPC_POST_REQUEST;
                parsePOSTRequest();
            } else {
                requestMode = FPRPC_CAML_REQUEST;
                parseCAMLRequest();
            }

        } else {
            throw new MalformedFPRPCRequest(httpRequest.getMethod() + " is not supported");
        }
    }

    protected void parseGETRequest() throws MalformedFPRPCRequest {
        parseSimpleParameters(FPRPCConts.CMD_PARAM);
    }

    protected void parsePOSTRequest() throws MalformedFPRPCRequest {
        parseSimpleParameters(FPRPCConts.METHOD_PARAM);
    }

    protected Map<String, String> extractVermeerEncodedParameters(HttpServletRequest httpRequest) throws IOException {
        Map<String, String> parameters = new HashMap<String, String>();

        InputStream input = httpRequest.getInputStream();

        int byt = input.read();
        boolean beginBinary = false;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int idx = 0;
        while (byt > 0 && !beginBinary) {
            buffer.write(byt);
            if (byt == 10) {
                beginBinary = true;
            } else {
                byt = input.read();
            }
            idx++;
        }

        String paramData = buffer.toString("utf-8");
        paramData = URLDecoder.decode(paramData, "utf-8");
        String[] parts = paramData.split("\\&");
        for (String part : parts) {
            int idx2 = part.indexOf("=");
            if (idx2 > 0) {
                String k = part.substring(0, idx2).trim();
                String v = part.substring(idx2 + 1).trim();
                if (v.startsWith("[")) {
                    Map<String, String> uParams = unpackParameters(v);
                    for (String sk : uParams.keySet()) {
                        parameters.put(k + "/" + sk, uParams.get(sk));
                    }
                } else {
                    parameters.put(k, v);
                }
            }
        }
        vermeerBinary = input;
        return parameters;
    }

    protected Map<String, String> unpackParameters(String packedParams) {
        Map<String, String> params = new HashMap<String, String>();
        packedParams = packedParams.substring(1, packedParams.length() - 1);
        String[] parts = packedParams.split("\\;");
        for (String part : parts) {
            String p[] = part.split("=");
            if (p.length == 2) {
                params.put(p[0].trim(), p[1].trim());
            } else {
                params.put(p[0].trim(), "");
            }
        }
        return params;
    }

    protected Map<String, String> extractUrlEncodedParameters(HttpServletRequest httpRequest) {

        Map<String, String> parameters = new HashMap<String, String>();

        Enumeration<String> pNames = httpRequest.getParameterNames();

        while (pNames.hasMoreElements()) {
            String key = pNames.nextElement().trim();
            String value = httpRequest.getParameter(key).trim();
            value = decodeParameterValue(value);
            parameters.put(key, value);
        }
        return parameters;
    }

    protected void parseSimpleParameters(String cmdName) throws MalformedFPRPCRequest {

        Map<String, String> parameters;

        if (FPRPCConts.VERMEER_ENCODED_CONTENT_TYPE.equals(httpRequest.getContentType())) {
            try {
                parameters = extractVermeerEncodedParameters(httpRequest);
            } catch (IOException e) {
                throw new MalformedFPRPCRequest("Error in Vermeer encoding parsing", e);
            }
        } else {
            parameters = extractUrlEncodedParameters(httpRequest);
        }

        String cmd = parameters.get(cmdName);
        if (cmd == null) {
            cmd = parameters.get("dialogview");
            if (cmd == null) {
                throw new MalformedFPRPCRequest("No Cmd parameter was found");
            }
        }
        cmd = cmd.replace("\n", "");

        if (cmd.contains(":")) {
            String[] parts = cmd.split("\\:");
            cmd = parts[0];
            version = parts[1];
        }

        parameters.remove(cmdName);
        FPRPCCall call = new FPRPCCall(cmd, parameters);
        calls = new ArrayList<FPRPCCall>();
        calls.add(call);
    }

    protected void parseCAMLRequest() throws MalformedFPRPCRequest {
        XMLReader reader;
        try {
            reader = CAMLHandler.getXMLReader();
            reader.parse(new InputSource(httpRequest.getInputStream()));
            calls = ((CAMLHandler) reader.getContentHandler()).getParsedCalls();
        } catch (Exception e) {
            throw new MalformedFPRPCRequest("Unable to parse CAML Request");
        }
    }

    public List<FPRPCCall> getCalls() {
        return calls;
    }

    public int getRequestMode() {
        return requestMode;
    }

    public String getVersion() {
        return version;
    }

    public InputStream getVermeerBinary() {
        return vermeerBinary;
    }

    public String getPrincipalName() {
        return principal != null ? principal.getName() : "anonymous";
    }

    @Override
    public String getBaseUrl(String fpDir) {

        StringBuilder sb = new StringBuilder();
        sb.append(super.getBaseUrl(fpDir));

        if (fpDir != null) {
            String sp = getSitePath();
            if (sp.startsWith("/")) {
                sp = sp.substring(1);
            }
            if (sp.endsWith("/")) {
                sp = sp.substring(0, sp.length() - 1);
            }
            if ("catalogs".equals(fpDir)) {
                if (!"".equals(sp)) {
                    sb.append(sp).append('/');
                }
                sb.append("_catalogs/");
            } else if ("layouts".equals(fpDir)) {
                if (!"".equals(sp)) {
                    sb.append(sp).append('/');
                }
                sb.append("_layouts/");
            }
        }
        return sb.toString();
    }

    protected String decodeParameterValue(String path) {
        String encoding = windowsEncoding;
        if (StringUtils.isEmpty(encoding)) {
            encoding = "ISO-8859-1";
        }
        try {
            path = new String(path.getBytes(encoding), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // nothing
        }
        return path;
    }

}
