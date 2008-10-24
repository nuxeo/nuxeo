<@extends src="base.ftl">
<@block name="title">Welcome to Nuxeo WebEngine!</@block>
<@block name="header"><h1><a href="${appPath}">Nuxeo WebEngine</a></h1></@block>
<@block name="content">
Hello <strong>${Context.principal.name}</strong>! This is the root of your web site.
</p>

<div id="mainContentBox">
Hi There, here is the wikis currently available :
<ul>
      <#list wikis as wiki>
        <li id="${wiki.ref}">
          <a href="${wiki.name}">${wiki.title}</a>
        </li>
      </#list>
</ul>

</div>

<div class="tip">
Your web root is <pre>${env.installDir}</pre>
</div>

</@block>

</@extends>
