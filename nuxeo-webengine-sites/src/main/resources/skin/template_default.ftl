<@theme>
<@block name="content">

<div style="width: 100%;height: 30%;background-color: black;text-align: left;padding-top: 30px;padding-left: 30px;padding-bottom: 30px" >
	<div style="vertical-align: middle;">
		<table >
			<tr style="vertical-align: top;">
				<td>
					<img style="width: 70px;height: 70px;" src="${This.path}/logo" alt="logo">
				</td>
				<td style="padding-left: 10px;text-align: left;">
					<div style="font-size: large;font-style: italic; color: white;">
						${siteName}
					</div>
					<div style="font-size: medium;font-style: italic; color: white;">
						${description} 
					</div>
				</td>
			</tr>
		</table>
	</div>
</div>

<center><h4>${welcomeText}</h4></center>
<!--
Logo : <img src="${This.path}/logo" alt="logo">
<hr>


Welcome Animation/Image</br>
<img src="${This.path}/welcomeMedia" alt="Welcome Media">

<hr>

${siteName}
Welcome Text:</br>
${welcomeText}
-->

<hr>
5 last modified/created web pages in the site
<hr>

<table border=1>
  <tr><th>Name<th>Description<th>Content
  <#list pages as p>
  <tr><td><a href="${This.path}/${p.path}"> ${p.name} &nbsp; </a>
  <td> ${p.description}&nbsp;
  <td>${p.content}&nbsp;
   </#list>
</table>


</@block>
</@theme>