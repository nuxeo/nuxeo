<@extends src="base.ftl">
<@block name="header"><h1><a href="${appPath}">FrontPage</a></h1></@block>
<@block name="content">

<h1>${Context.getMessage("label.create.wiki.page")}</h1>

<p>
${Context.getMessage("label.click")} <a href="${This.path}/create/${This.nextSegment}">${Context.getMessage("label.here")}</a>
${Context.getMessage("label.click.here.to")} <em>${This.nextSegment}</em>.
</p>

</@block>
</@extends>
