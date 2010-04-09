<#if Root.currentDistribution!=null>

<h3>Browse distributions</h3>
<table border=0>
<tr>
      <td colspan="4" style="font-weight:bold">
      <A href="${Root.path}"> Distributions </A>
      </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td colspan="4">
      <A href="${Root.path}/${distId}/listBundleGroups">
       Artifacts groups
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td colspan="3">
      <A href="${Root.path}/${distId}/listBundles">
       Bundles
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td colspan="2">
      <A href="${Root.path}/${distId}/listComponents">
       Components
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td>
      <A href="${Root.path}/${distId}/listServices">
       Services
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td >
      <A href="${Root.path}/${distId}/listExtensionPoints">
       ExtensionPoints
      </A>
    </td>
</tr>
<tr>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td width="10">&nbsp;</td>
    <td>
      <A href="${Root.path}/${distId}/listContributions">
       Contributions
      </A>
    </td>
</tr>
</table>
<br/>
<h3>Browse documentation</h3>
<table border=0>
<tr>
      <td colspan="4" style="font-weight:bold">
      <A href="${Root.path}/${distId}/doc"> FAQ and How to </A>
      </td>
</tr>
</table>

</#if>