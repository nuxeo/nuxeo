package org.jboss.resteasy.specimpl.patch;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.PathSegmentImpl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UriInfoImpl implements UriInfo
{
   private String path;
   private String encodedPath;
   private MultivaluedMap<String, String> queryParameters;
   private MultivaluedMap<String, String> encodedQueryParameters;
   private MultivaluedMap<String, String> pathParameters;
   private MultivaluedMap<String, String> encodedPathParameters;
   private MultivaluedMap<String, PathSegment[]> pathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
   private MultivaluedMap<String, PathSegment[]> encodedPathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
   
   private List<PathSegment> pathSegments;
   private List<PathSegment> encodedPathSegments;
   private URI absolutePath;
   private URI absolutePathWithQueryString;
   private URI baseURI;
   private String queryString;
   private List<String> matchedUris;
   private List<String> encodedMatchedUris;
   private List<Object> ancestors;

   
   public static UriInfoImpl create(HttpServletRequest req) {
       StringBuilder serverPart = new StringBuilder ();
       String scheme = req.getScheme ();
       int port = req.getServerPort ();
       String urlPath = req.getRequestURI();
       if (urlPath.length() == 0) {
           urlPath = "/";
       }
       String contextPath = req.getContextPath(); // encoded
       String servletPath = req.getServletPath(); // decoded
       StringBuilder buf = new StringBuilder().append(contextPath).append(servletPath);
       String basePath =  buf.length() == 0 ? "/" : buf.toString();
       // the encoded pathInfo
       int len = basePath.length();
       String pathInfo = len == urlPath.length() ? "" : urlPath.substring(len);
       serverPart.append(scheme).append("://").append(req.getServerName());
       if ((scheme.equals ("http") && port != 80)
               || (scheme.equals ("https") && port != 443)) {
           serverPart.append (':');
           serverPart.append (req.getServerPort ());
       }
       return new UriInfoImpl(serverPart, urlPath, basePath, pathInfo, req.getQueryString());
   }
   

   
   /**
    * All paths are in encoded form.
    * @param serverPart - the complete URI (in encoded form) without query string
    * @urlPath - the complete path in encoded form without query string. Allways starts with /  
    * @param basePath starts with /. Doesn't end with /. Never null or empty.
    * @param path Never null. Can be an empty string. If non empty starts in / but never end in / 
    * @param query query string or null if none
    */
   public UriInfoImpl(StringBuilder serverPart, String urlPath, String basePath, String path, String queryString) {
       StringBuilder sb = new StringBuilder();
       sb.append(serverPart).append(basePath);
       this.baseURI = URI.create(sb.toString());
       sb.setLength(serverPart.length());
       this.absolutePath = URI.create(sb.append(urlPath).toString());
       this.encodedPath = path;
       this.path = UriComponent.decode(encodedPath, UriComponent.Type.PATH);
       
       this.queryParameters = new MultivaluedMapImpl<String, String>();
       this.encodedQueryParameters = new MultivaluedMapImpl<String, String>();
       extractParameters(queryString);
       this.encodedPathParameters = new MultivaluedMapImpl<String, String>();
       this.pathParameters = new MultivaluedMapImpl<String, String>();
       this.encodedPathSegments = split(path, '/');
       this.pathSegments = new ArrayList<PathSegment>(encodedPathSegments.size());
       for (PathSegment segment : encodedPathSegments)
       {
             pathSegments.add(new PathSegmentImpl(UriComponent.decode(((PathSegmentImpl) segment).getOriginal(), UriComponent.Type.PATH_SEGMENT)));
       }
       this.queryString = queryString;
       String absolutePathAsString = absolutePath.toString();
       if (queryString == null) {
           this.absolutePathWithQueryString = absolutePath;
       }
       else
       {           
           sb.setLength(0); sb.append(absolutePathAsString).append('?').append(queryString);
           this.absolutePathWithQueryString = URI.create(sb.toString());
       }
   }
   

   public static List<PathSegment> split(String str, char delimiter) {
       List<PathSegment> ar = new ArrayList<PathSegment>();
       int len = str.length(); 
       if (len == 0) {
           return ar;
       }
       int s = 0;
       if (str.charAt(0) == delimiter) {
           if (len == 1) {
               return ar;
           } else {
               s = 1;
           }
       }
       int e = str.indexOf(delimiter, s);
       if (e == -1) {
           if (s > 0) {
               str = str.substring(s);
           }
           ar.add(new PathSegmentImpl(str));
           return ar;
       }
       do {
           String segment = str.substring(s, e);
           ar.add(new PathSegmentImpl(segment));
           s = e + 1;
           e = str.indexOf(delimiter, s);
       } while (e != -1);

       if (s < len) {
           String segment = str.substring(s);
           ar.add(new PathSegmentImpl(segment));
       } // ignore trailing /
       return ar;
   }

   /**
    * @param absolutePath
    * @param path         decoded equivalent to HttpServletRequest.getPathInfo()
    * @param queryString  encoded query string of request
    */
   public UriInfoImpl(URI absolutePath, String path, String queryString)
   {
      this(absolutePath, path, queryString, PathSegmentImpl.parseSegments(path));
   }
   
   public UriInfoImpl(URI absolutePath, String encodedPath, String queryString, List<PathSegment> encodedPathSegments)
   {
      this.encodedPath = encodedPath;
      try
      {
         this.path = URLDecoder.decode(encodedPath, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
      //System.out.println("path: " + path);
      //System.out.println("encodedPath: " + encodedPath);
      this.absolutePath = absolutePath;
      this.queryParameters = new MultivaluedMapImpl<String, String>();
      this.encodedQueryParameters = new MultivaluedMapImpl<String, String>();
      extractParameters(queryString);
      this.encodedPathParameters = new MultivaluedMapImpl<String, String>();
      this.pathParameters = new MultivaluedMapImpl<String, String>();
      this.encodedPathSegments = encodedPathSegments;
      this.pathSegments = new ArrayList<PathSegment>(encodedPathSegments.size());
      for (PathSegment segment : encodedPathSegments)
      {
         try
         {
            pathSegments.add(new PathSegmentImpl(URLDecoder.decode(((PathSegmentImpl) segment).getOriginal(), "UTF-8")));
         }
         catch (UnsupportedEncodingException e)
         {
            throw new RuntimeException(e);
         }
      }


      this.queryString = queryString;


      if (queryString == null) this.absolutePathWithQueryString = absolutePath;
      else
      {
         this.absolutePathWithQueryString = URI.create(absolutePath.toString() + "?" + queryString);
      }
      if (encodedPath.trim().equals("")) baseURI = absolutePath;
      else
      {
         String abs = absolutePath.getRawPath();
         abs = abs.substring(0, abs.indexOf(encodedPath));
         if (!abs.endsWith("/")) abs += "/";
//         System.out.println("abs: " + abs);
//         System.out.println("absolutePath: " + absolutePath);
//         System.out.println("encodedPath: " + encodedPath);
         try
         {
            baseURI = UriBuilder.fromUri(absolutePath).replacePath(abs).build();

         }
         catch (Exception e)
         {
            throw new RuntimeException("URI value was: " + abs + " encodedPath: " + encodedPath, e);
         }
      }
   }

   public UriInfoImpl clone()
   {
      return new UriInfoImpl(absolutePath, encodedPath, queryString, encodedPathSegments);
   }

   public String getPath()
   {
      return path;
   }

   public String getPath(boolean decode)
   {
      if (decode) return getPath();
      return encodedPath;
   }

   public List<PathSegment> getPathSegments()
   {
      if (pathSegments != null) return pathSegments;
      pathSegments = PathSegmentImpl.parseSegments(getPath());
      return pathSegments;
   }

   public List<PathSegment> getPathSegments(boolean decode)
   {
      if (decode) return getPathSegments();
      return encodedPathSegments;
   }

   public URI getRequestUri()
   {
      return absolutePathWithQueryString;
   }

   public UriBuilder getRequestUriBuilder()
   {
      return UriBuilder.fromUri(absolutePathWithQueryString);
   }

   public URI getAbsolutePath()
   {
      return absolutePath;
   }

   public UriBuilder getAbsolutePathBuilder()
   {
      return UriBuilder.fromUri(absolutePath);
   }

   public URI getBaseUri()
   {
      return baseURI;
   }

   public UriBuilder getBaseUriBuilder()
   {
      return UriBuilder.fromUri(baseURI);
   }

   public MultivaluedMap<String, String> getPathParameters()
   {
      return pathParameters;
   }

   public void addEncodedPathParameter(String name, String value)
   {
      encodedPathParameters.add(name, value);
      try
      {
         pathParameters.add(name, URLDecoder.decode(value, "UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException(e);
      }
   }

   public MultivaluedMap<String, PathSegment[]> getEncodedPathParameterPathSegments()
   {
      return encodedPathParameterPathSegments;
   }

   public MultivaluedMap<String, PathSegment[]> getPathParameterPathSegments()
   {
      return pathParameterPathSegments;
   }

   public MultivaluedMap<String, String> getPathParameters(boolean decode)
   {
      if (decode) return getPathParameters();
      return encodedPathParameters;
   }

   public MultivaluedMap<String, String> getQueryParameters()
   {
      return queryParameters;
   }

   public MultivaluedMap<String, String> getQueryParameters(boolean decode)
   {
      if (decode) return queryParameters;
      else return encodedQueryParameters;
   }

   protected void extractParameters(String queryString)
   {
      if (queryString == null || queryString.equals("")) return;

      String[] params = queryString.split("&");

      for (String param : params)
      {
         if (param.indexOf('=') >= 0)
         {
            String[] nv = param.split("=");
            try
            {
               String name = URLDecoder.decode(nv[0], "UTF-8");
               String val = nv.length > 1 ? nv[1] : "";
               encodedQueryParameters.add(name, val);
               queryParameters.add(name, URLDecoder.decode(val, "UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
         else
         {
            try
            {
               String name = URLDecoder.decode(param, "UTF-8");
               encodedQueryParameters.add(name, "");
               queryParameters.add(name, "");
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
   }

   public List<String> getMatchedURIs(boolean decode)
   {
      if (decode)
      {
         if (matchedUris == null) matchedUris = new ArrayList<String>();
         return matchedUris;
      }
      else
      {
         if (encodedMatchedUris == null) encodedMatchedUris = new ArrayList<String>();
         return matchedUris;
      }
   }

   public List<String> getMatchedURIs()
   {
      return getMatchedURIs(true);
   }

   public List<Object> getMatchedResources()
   {
      if (ancestors == null) ancestors = new ArrayList<Object>();
      return ancestors;
   }


   public void pushCurrentResource(Object resource)
   {
      if (ancestors == null) ancestors = new ArrayList<Object>();
      ancestors.add(0, resource);
   }

   public void popCurrentResource()
   {
      if (ancestors != null && ancestors.size() > 0)
      {
         ancestors.remove(0);
      }
   }

   public void pushMatchedURI(String encoded, String decoded)
   {
      if (encodedMatchedUris == null) encodedMatchedUris = new ArrayList<String>();
      encodedMatchedUris.add(0, encoded);

      if (matchedUris == null) matchedUris = new ArrayList<String>();
      matchedUris.add(0, decoded);
   }

   public void popMatchedURI()
   {
      if (encodedMatchedUris != null && encodedMatchedUris.size() > 0)
      {
         encodedMatchedUris.remove(0);
      }
      if (matchedUris != null && matchedUris.size() > 0)
      {
         matchedUris.remove(0);
      }
   }

   public String getConnegExtension()
   {
      throw new RuntimeException("NOT IMPLEMENTED YET");
   }
}
