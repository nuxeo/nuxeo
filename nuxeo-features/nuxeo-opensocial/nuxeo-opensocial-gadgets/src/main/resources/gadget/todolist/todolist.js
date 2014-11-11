var todolist; // This is a json object of bookmark items
    var edited;
    var prefs;
    var permission;

    function saveToDoList() {
      prefs.set("todolist", JSON.stringify(todolist));
      var ver = navigator.appVersion;
      if (ver.indexOf("MSIE") == -1)
      _gel("newNameInput").focus();
    }


    function editItem(indexItem, name, owner, deadline)
    {
      if (indexItem > -1 && indexItem < numToDoList()-1)
      {
      todolist.array[indexItem] = {
        "name" :name,
        "deadline" :deadline,
        "owner" :owner
      };
      saveToDoList();
      }

    }

    function testTypeDate(dateEntree)
    {
      tst=false;
      try
      {rc=dateEntree.split("/");nd=new Date(rc[2],(rc[1]-1),rc[0]);
      tst=(rc[2]>1800&&rc[2]<2200&&rc[2]==nd.getFullYear()&&rc[1]==(nd.getMonth()+1)&&rc[0]==nd.getDate());
      } catch(e) {}
      return tst;
    }

    function addItem(name, owner, deadline) {

      var name = _trim(name);

      hasError = false
      if (name == "")
      {
      _gel("labelIntitule").className="labelError";
      hasError = true;
      }

      if(deadline!= "" && !testTypeDate(deadline))
      {
        _gel("labelEcheance").className="labelError";
        hasError = true;
      }

      if (hasError)
          return;

      _gel("newNameInput").value = "";
      _gel("newDeadLineInput").value = "";
      _gel("newOwnerInput").value = "";
      _gel("labelIntitule").className="label";
      _gel("labelEcheance").className="label";


      if (document.newItemForm.action.value == "edit")
      {
      todolist.array[document.newItemForm.indexTab.value] = {
          "name" :name,
          "deadline" :deadline,
          "owner" :owner
        };
      }
      else
      {
      todolist.array[numToDoList()] = {
        "name" :name,
        "deadline" :deadline,
        "owner" :owner
      };
      }

      toogleForms();

      //sortByName(numToDoList() - 1);
      createTable();
      saveToDoList();
      gadgets.window.adjustHeight();
      return false;
    }


    function editFormItem(number){

      toogleForms("edit");

      document.newItemForm.indexTab.value= number;
      document.newItemForm.newNameInput.value= todolist.array[number].name;
      document.newItemForm.newDeadLineInput.value= todolist.array[number].deadline;
      document.newItemForm.newOwnerInput.value= todolist.array[number].owner;


    }

    function deleteItem(number) {
      if (edited)
      return;
      if (!confirm("Etes-vous sûr de vouloir supprimer cette tâche ?"))
      return;
      var beginning = todolist.array.slice(0, number);
      var end = todolist.array.slice(number + 1, numToDoList());
      todolist.array = beginning.concat(end);
      createTable();
      saveToDoList();
      gadgets.window.adjustHeight();
    }

    function swapToDoList(to, from) {
      var temp = todolist.array[to];
      todolist.array[to] = todolist.array[from];
      todolist.array[from] = temp;
    }

    function sortByName(number) {
      var bookmark = todolist.array[number];
      ;
      var lastName = bookmark.name;
      for (i = number - 1; i >= 0; i--) {
      var currentItem = todolist.array[i];
      if (currentItem == null)
        break;
      var currentName = currentItem.name;
      if (currentName.toUpperCase() <= lastName.toUpperCase())
        break;
      swapToDoList(i + 1, i);
      }
    }

    function rowClass(number) {
    if (number % 2 != 0)
      return " class=odd ";
    else
      return " class=even ";
  }


    function createAddItemButton(){

      var html ="<a class=\"addItem\" href=\"javascript: toogleForms('create')\" style=\"text-decoration:none; font-size: 80%; color: black;\">Ajouter</a>";

      _gel("addItemIcon").innerHTML = html;
      gadgets.window.adjustHeight();

    }

    function createTable() {
      var html = "<table cellspacing=0 id=todolistTable>";
      if (numToDoList() > 0)
        html = html + "<thead> <tr class=\"entete\"> <th>&nbsp;&nbsp;Intitulé</th> <th>Attribuée à</th> <th>Echéance</th><th>&nbsp;</th><th>&nbsp;</th> </tr></thead><tbody> ";
      for (i = 0; i < numToDoList(); i++) {
      if (todolist.array[i] == null)
        break;
      var deadline = todolist.array[i].deadline;
      var name = todolist.array[i].name;
      var owner = todolist.array[i].owner;
      html = html + createRow(i, deadline, name,owner);
      }
      if (numToDoList() > 0)
        html = html + "</tbody>";
      html = html + "</table>";
      _gel("todolistDiv").innerHTML = html;

      jQuery("#todolistTable").tablesorter(
      {
            // pass the headers argument and assing a object
            headers: {
                2:{sorter:'dates'},
                // assign the secound column (we start counting zero)
                3: {
                    // disable it by setting the property sorter to false
                    sorter: false
                },
                // assign the third column (we start counting zero)
                4: {
                    // disable it by setting the property sorter to false
                    sorter: false
                }
            }
        }
      );
      gadgets.window.adjustHeight();
    }

    function getDate(strDate){
      day = strDate.substring(0,2);
    month = strDate.substring(3,5);
    year = strDate.substring(6,10);
    d = new Date();
    d.setDate(day);
    d.setMonth(month);
    d.setFullYear(year);
    return d;
    }

    //Retourne:
    //   0 si date_1=date_2
    //   1 si date_1>date_2
    //  -1 si date_1<date_2
    function compare(date_1, date_2){
      diff = date_1.getTime()-date_2.getTime();
      return (diff==0?diff:diff/Math.abs(diff));
    }


    function createRow(number, deadline, name,owner) {
      dateEcheance = new Date(deadline.substring(6,10), parseInt(deadline.substring(3,5))-1, deadline.substring(0,2));
      datedujour = new Date();
      datedujour2 = new Date(datedujour.getFullYear(),datedujour.getMonth(),datedujour.getDate());

      if (compare(dateEcheance, datedujour2) == 0)
      {
        classLigne="now";
      }
      else
      {
        if (compare(dateEcheance, datedujour2) == 1)
        {
          classLigne="futur";
        }
        else
        {
          classLigne="past";
        }
      }

      var html = "<tr id=\"row" + number + "\">"
        + "<td class=\"name_td "+classLigne+"\">" + "&nbsp;&nbsp;" + name + "</td>"+ "<td class=\"owner_td "+classLigne+"\">"
        + "&nbsp;&nbsp;" + owner + "</td>"+ "<td class=\"deadline_td "+classLigne+"\">" +deadline + "</td>" + "<td class=\""+classLigne+"\">"
        + createEdit(number) + "</td>" + "<td style=\"width:16px;\" class=\""+classLigne+"\">"
        + createDelete(number) + "</td>" + "</tr>";
      return html;
    }

    function escapeName(name) {
      name = name.replace(/&/g, "&#38;")
      name = name.replace(/</g, "&#60;")
      name = name.replace(/>/g, "&#62;")
      name = name.replace(/"/g, "&#34;")
      name = name.replace(/'/g, "&#39;")
      return name;
    }

    function createName(number, name) {
      var html = "<a class=name_a" + " href=\"javascript:editName(" + number
        + ")\">" + escapeName(name) + "</a>";
      return html;
    }

    function createEdit(number)
    {
      var html;
      if (permission == true)
      {
      html = "<a class=\"editLink\" title=\"Editer la tâche\" href=\"javascript:editFormItem("
        + number + ")\">" + "</a>";
      }
      else
      {
        html="";
      }
      return html;
    }

    function createDelete(number) {
      var html;
      if (permission == true)
      {
      html = "<a class=\"deleteLink\" title=\"Supprimer la tâche\" href=\"javascript:deleteItem("
        + number + ")\">" + "</a>";
      }
      else
      {
      html="";
      }
      return html;
    }

    function numToDoList() {
      return todolist.array.length;
    }

    function toogleForms(action)
    {
      document.newItemForm.action.value= action;
      document.newItemForm.newNameInput.value= "";
      document.newItemForm.newDeadLineInput.value= "";
      document.newItemForm.newOwnerInput.value= "";
      _gel("labelIntitule").className="label";
      _gel("labelEcheance").className="label";


      toggleLayer('addItemIcon');
      toggleLayer('addItemForm');

    }

    function toggleLayer(whichLayer) {
      if (document.getElementById) {
      // this is the way the standards work
      var style2 = document.getElementById(whichLayer).style;
      if (whichLayer == "addItemIcon") {
        style2.visibility = (style2.visibility == "visible") ? "hidden"
          : "visible";
      } else {
        style2.display = (style2.display == "block") ? "none" : "block";
      }
      } else if (document.all) {
      // this is the way old msie versions work
      var style2 = document.all[whichLayer].style;
      style2.display = (style2.display == "block") ? "none" : "block";
      } else if (document.layers) {
      // this is the way nn4 works
      var style2 = document.layers[whichLayer].style;
      style2.display = (style2.display == "block") ? "none" : "block";
      }

      gadgets.window.adjustHeight();
    }



    jQuery.tablesorter.addParser({
        // set a unique id
        id: 'dates',
        is: function(s) {
                // return false so this parser is not auto detected
                return false;
        },
        format: function(s) {
            // split
            var a = s.split('/');
            return new Date(a.reverse().join("/")).getTime();
        },
        // set type, either numeric or text
        type: 'numeric'
});