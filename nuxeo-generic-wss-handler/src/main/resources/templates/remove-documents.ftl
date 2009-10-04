<html><head><title>vermeer RPC packet</title></head>
<body>
<p>method=remove documents:${request.version}
<p>message=successfully removed documents
<p>removed_docs=
<ul>
<#list removedDocUrls as url>
<ul>
<li>document_name=${url}
<li>meta_info=
<ul>
</ul>
</ul>
</#list>
</ul>
<p>removed_dirs=
<ul>
<#list removedDirUrls as url>
<ul>
<li>document_name=${url}
<li>meta_info=
<ul>
</ul>
</ul>
</#list>
</ul>
<p>failed_docs=
<ul>
<#list failedDocUrls as url>
<ul>
<li>document_name=${url}
<li>meta_info=
<ul>
</ul>
</ul>
</#list>
</ul>
<p>failed_dirs=
<ul>
<#list failedDirUrls as url>
<ul>
<li>document_name=${url}
<li>meta_info=
<ul>
</ul>
</ul>
</#list>
</ul>
</body>
</html>