<#if Context.principal.isAnonymous()>
  <a href="/nuxeo/login.jsp?forceAnonymousLogin=true&requestedUrl=${Root.path}" id="login">Login</a>
<#else>
  <span id="logstate">${Context.principal.name}</span>
  <a href="/nuxeo/logout" id="logout">Logout</a>
</#if>
