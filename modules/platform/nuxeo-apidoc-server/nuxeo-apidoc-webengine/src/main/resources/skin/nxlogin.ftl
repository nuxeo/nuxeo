<#if Context.principal.isAnonymous()>
  <a href="/${Root.webappName}/login.jsp?forceAnonymousLogin=true&requestedUrl=${Root.path}" id="login">Login</a>
<#else>
  <span id="logstate">${Context.principal.name}</span>
  <a href="/${Root.webappName}/logout" id="logout">Logout</a>
</#if>
