package org.nuxeo.wss.servlet;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Vitalii Siryi
 */
public class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

    protected final Map<String, String> headers = new HashMap<String, String>();

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
            String headerName = String.valueOf(e.nextElement());

            if (StringUtils.isNotEmpty(headerName)) {
                String value = request.getHeader(headerName);
                headers.put(headerName.toLowerCase(), value);
            }
        }
    }

    @Override
    public String getHeader(String name) {
        if (StringUtils.isNotEmpty(name)) {
            return headers.get(name.toLowerCase());
        } else {
            return null;
        }
    }

    public void setHeader(String name, String value) {
        if (StringUtils.isNotEmpty(name)) {
            headers.put(name.toLowerCase(), value);
        }
    }

    @Override
    public Enumeration getHeaderNames() {
        return new SetEnumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        Set<String> set = new HashSet<String>();
        String value = headers.get(name);
        if(StringUtils.isNotEmpty(value)){
            set.add(value);
        }
        return new SetEnumeration(set);
    }

    class SetEnumeration implements Enumeration<String> {

        private final Iterator<String> iterator;

        public SetEnumeration(Set<String> set) {
            iterator = new ArrayList<String>(set).iterator();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public String nextElement() {
            return iterator.next();
        }

    }


}
