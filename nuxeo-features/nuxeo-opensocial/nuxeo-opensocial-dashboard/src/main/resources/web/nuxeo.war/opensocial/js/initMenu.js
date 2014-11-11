

jQuery(document).ready( function() {

   activeMenu();
   // toggle des icones des onglets permettant d'afficher les menus lorsqu'on clique dessus
  jQuery('.showPreferences').click( function(){
    return false;
  });

});

//permet de charger les menus pour chaque onglet
function activeMenu(){
   var options = {minWidth: 120, copyClassAttr: true, showDelay: 2000, hideDelay: 2000, offsetTop: 5};

   jQuery(".showPreferences").each(function (){
     var imgId = '#' + jQuery(this).attr('id');
     var ulId = '#' + 'ul-' + jQuery(this).attr('id');
     jQuery(imgId).menu(options, ulId);
   });
};