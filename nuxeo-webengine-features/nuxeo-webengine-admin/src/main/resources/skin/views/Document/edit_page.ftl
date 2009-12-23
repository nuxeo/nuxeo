<#if This.isRoot()>
<h2><#if Root.parent>${Root.document.title}<#else>Repository</#if></h2>
<#else>
<h2><a href="${This.previous.path}"><img src="${skinPath}/image/up_nav.gif" alt="Up" border="0"/></a> ${Document.title}</h2>
</#if>

<div id="mainContentBox">

<form action="${This.path}/@put" method="POST">
<table class="formFill">
    <tbody>
        <!--tr>
            <td>Name:</td>
            <td><input type="text" name="name" value="" size="40"></td>
        </tr>
        <tr>
            <td>Type:</td>
            <td>
              <select name="doctype">
              <#list API.getSortedDocumentTypes() as type>
                <option value="${type.name}">${type.name}</option>
              </#list>
              </select>
            </td>
        </tr-->
        <tr>
            <td>Title:</td>
            <td><input type="text" name="dc:title" value="${Document.title}" size="40"/></td>
        </tr>
        <tr>
            <td colspan="2">Description</td>
        </tr>
        <tr>
            <td colspan="2"><textarea name="dc:description" cols="54">${Document["dc:description"]}</textarea></td>
        </tr>
        <#if This.hasFacet("Versionable")>
        <tr>
            <td colspan="2">
                <p class="entryEditOptions">
                    Version increment:
                    <input class="radioButton" type="radio" name="versioning" value="major" checked> Major
                    &nbsp;&nbsp;
                    <input class="radioButton" type="radio" name="versioning" value="minor"/> Minor
                    &nbsp;&nbsp;
                    <input class="radioButton" type="radio" name="versioning" value=""/> None
                </p>
            </td>
        </tr>
        </#if>
        <tr>
          <td colspan="2" align="right"><input type="submit" value="Save" class="buttonsGadget"/></td>
        </tr>

    </tbody>
</table>
</form>
</div>
