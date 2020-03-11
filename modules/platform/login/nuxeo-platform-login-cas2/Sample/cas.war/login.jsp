<%@ include file="header.jsp" %>

<p>
<% if (request.getAttribute("edu.yale.its.tp.cas.badUsernameOrPassword")
       != null) { %>
<font color="red">Désolé, vous avez rentré un identifiant ou un mot de passe invalide. <br />Reessayer s'il vous plait.</font>
  <% } else if (request.getAttribute("edu.yale.its.tp.cas.service") == null) { %>
  Vous devez vous identifier maintenant &agrave; CAS avant d'acc&egrave;der
  aux diff&eacute;rentes applications.
  <% } else { %>
  Vous tentez d'acc&egrave;der &agrave; un site qui demande une authentification.
  <% } %>
</p>
</font>

<font face="Arial,Helvetica">
<p> Entrez votre identifiant et votre mot de passe ci-desous; puis cli quez sur
  le bouton <b>OK</b>  ou bien cliquer <a href="http://localhost:8080/nuxeo">ici</a> pour vous connecter sur Nuxeo en utilisateur anonyme</p>
</font>


        <table bgcolor="#FFFFAA" align="center"><tr><td>

        <table border="0" cellpadding="0" cellspacing="5">


        <tr>
        <td><font face="Arial,Helvetica"><b>Identifiant :</b></td>
        <td>
        <form method="post" name="login_form">
        <input type="text" name="username"></td>
        </tr>

        <tr>
        <td><font face="Arial,Helvetica"><b>Mot de passe :</b></td>
        <td><input type="password" name="password"></td>
        </tr>


        <tr>
        <td colspan="2" align="right">
        <input type="submit" value=" Ok ">
        </form>
        </td>
        </tr>

        </td></tr></table>

        </td></tr></table>

</td></tr>

<tr><td colspan="2">
<center>
<font color="red" face="Arial,Helvetica">
<i><b>Authentification CAS 2</b></i>
</font>
</center>
</td></tr>


<tr><td colspan="2">
    <p> <font face="Arial,Helvetica" size="1"> ... Le nom de l'utilisateur ainsi
      que le mot de passe vous permettront de vous connecter automatiquement &agrave;
      l'ensemble des applications auquelles vous avez acc&egrave;s. Pour vous
      decoonecter..... </p>
<p>
<a target="new" href="">Aide sur la connexion</a>
</p>

</td></tr>

</table>
</table>
</table>
