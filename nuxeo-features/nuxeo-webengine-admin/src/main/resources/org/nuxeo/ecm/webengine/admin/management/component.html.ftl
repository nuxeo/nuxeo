<html>
<head>
<title>Component - ${component.name.name}</title>
</head>
<body>
<h2>Component <a href=".">${component.name.name}</a></h2>
	<blockquote>
	${component.documentation}
	</blockquote>

<p>
<b>State:</b> ${component.state}
<br> 
<b>Persistent:</b> ${component.persistent?string}	
<br>
</p>

<b><a href="xpoints">Extension Points</a></b>
<ul>
<#list component.extensionPoints as xp>
<li> <a href="xpoints/${xp.name}">${xp.name}</a><br>
<blocquote>${xp.documentation}</blockquote>
</#list>
</ul>
<b><a href="contribs">Contributions</a></b>
<ul>
<#list component.extensions as xt>
<li> <b>Id:</b> ${xt.id} 
<br><b>Target Componnent:</b> ${xt.targetComponent}. 
<br><b>Target Extension Point:</b> ${xt.extensionPoint} 
<blockquote>${xt.documentation}</blockquote>
</#list>
</ul>
</body>
</html>