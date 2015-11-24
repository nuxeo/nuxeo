<#if !insideNuxeo>
  <Require feature="oauthpopup" />
   <OAuth>
    <Service name="nuxeo">
      <Access url="${serverSideBaseUrl}oauth/access-token" method="POST" />
      <Request url="${serverSideBaseUrl}oauth/request-token" method="POST" />
      <Authorization url="${serverSideBaseUrl}oauth/authorize" />
    </Service>
  </OAuth>
</#if>