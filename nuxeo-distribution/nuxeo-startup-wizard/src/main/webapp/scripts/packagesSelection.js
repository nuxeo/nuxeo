var nuxeo = nuxeo || {};
(function(nuxeo) {

  // store all packages
  var packages = {};
  // packages to children packages
  var packagesToChildren = {};
  // packages to parent package
  var packagesToParent = {};

  /**
   * Initialize the packages selection from the packages definition server side.
   */
  function initPackagesSelection(packagesURL) {
    $.get(packagesURL, function (data) {
      registerPackages(data.children);
      buildPackagesSelectionUI(data);
    });
  }

  /**
   * Register in memory all packages.
   */
  function registerPackages(pkgs, parentPackageId) {
    pkgs.forEach(function(pkg) {
      packages[pkg.id] = pkg;

      // handle parent package
      if (parentPackageId) {
        var arr = packagesToParent[pkg.id] || [];
        arr.push(parentPackageId);
        packagesToParent[pkg.id] = arr;
      }

      // handle implies packages
      if (pkg.implies && pkg.implies.length > 0) {
        pkg.implies.forEach(function(imply) {
          var children = packagesToChildren[imply] || [];
          children.push(pkg.id);
          packagesToChildren[imply] = children;
        });
      }

      // register children packages
      if (pkg.children && pkg.children.length > 0) {
        registerPackages(pkg.children, pkg.id);
      }
    });
  }

  /**
   * Build the packages selection UI.
   */
  function buildPackagesSelectionUI(packages) {
    var $tree = $('#tree');
    packages.children.forEach(function (child) {
      $tree.append($('<h2>' + child.label + '</h2>'));
      var $pkgContainer = $('<div class="packageContainer" package-id="' + child.id + '" />');
      child.children.forEach(function (pkg) {
        var selected = pkg.selected === 'true';
        var $input = child.selectionType === 'single'
          ? createRadioButton(child, pkg, selected)
          : createCheckBox(pkg, selected);
        var classes = 'package' + (selected ? ' checked' : '');
        var $pkg = $('<div class="' + classes + '" package-id="' + pkg.id + '" />');
        $pkg.click(handlePackageClick);
        $pkg.append($input);
        $pkg.append(createPackageDescription(pkg));
        $pkgContainer.append($pkg);
      });
      $tree.append($pkgContainer);
    });
  }

  /**
   * Creates a radio button for the given package when the selection type is "single".
   */
  function createRadioButton(parentPkg, pkg, selected) {
    var radioButton = '<input type="radio" ';
    radioButton += ' name="' + parentPkg.id + '" ';
    radioButton += ' value="' + pkg.id + '" ';
    radioButton += ' id="pkg_' + pkg.id + '" ';
    radioButton += ' title="' + pkg.label + '" ';
    radioButton += ' package-id="' + pkg.id + '" ';
    radioButton += '/>';
    radioButton += '<label for=';
    radioButton += '"pkg_' + pkg.id + '">';
    radioButton += pkg.label;
    radioButton += '</label>';

    radioButton = $(radioButton);
    if (selected) {
      radioButton.attr('checked', true);
    }

    return radioButton;
  }

  /**
   * Creates a checkbox for the given package when the selection type is "multiple" or undefined.
   */
  function createCheckBox(pkg, selected) {
    var checkBox = '<input type="checkbox" ';
    checkBox += ' name="' + pkg.id + '" ';
    checkBox += ' value="' + pkg.id + '" ';
    checkBox += ' id="pkg_' + pkg.id + '" ';
    checkBox += ' title="' + pkg.label + '" ';
    checkBox += ' package-id="' + pkg.id + '" ';
    checkBox += '/>';
    checkBox += '<label for=';
    checkBox += '"pkg_' + pkg.id + '">';
    checkBox += pkg.label;
    checkBox += '</label>';

    checkBox = $(checkBox);
    if (selected) {
      checkBox.attr('checked', true);
    }

    return checkBox;
  }

  function createPackageDescription(pkg) {
    var html = '';
    if (pkg.description && pkg.description !== 'null') {
      html += '<p class="packageDescription">';
      html += pkg.description;
      html += '</p>';
    }
    return $(html);
  }

  function handlePackageClick() {
    var $i = $(this).find('input[type="checkbox"], input[type="radio"]');
    var checked = !$i.attr('checked');
    var packageId = $i.attr('package-id');

    $i.attr('checked', checked);
    if (checked) {
      selectPackage(packageId);
    } else {
      unSelectPackage(packageId);
    }
  }

  /**
   * Selects the given package and all its 'implies' packages.
   */
  function selectPackage(id) {
    var pkg = packages[id];
    if (!pkg) {
      return;
    }

    // select the package
    pkg.selected = true;
    $('input[package-id="' + id + '"]').attr('checked', true);
    $('div[package-id="' + id + '"]').attr('class', 'package checked')

    // unselect all siblings if needed based on the exclusive attribute
    var parentPackage = packages[packagesToParent[id]];
    console.log(parentPackage);
    if (parentPackage) {
      parentPackage.children.forEach(function(p) {
        // if this package or the other one is exclusive
        if (pkg.id !== p.id && (pkg.exclusive === 'true' || p.exclusive === 'true')) {
          unSelectPackage(p.id);
        }
      });
    }

    // select also implies packages
    pkg.implies.forEach(function(p) {
      selectPackage(p);
    });
  }

  /**
   * Un-selects the given package and all its 'implies' packages.
   */
  function unSelectPackage(id) {
    var pkg = packages[id];
    if (!pkg) {
      return;
    }

    // check if we can really un-select the package
    if (!canUnSelectPackage(id)) {
      return;
    }

    // un-select package
    pkg.selected = false;
    $('input[package-id="' + id + '"]').attr('checked', false);
    $('div[package-id="' + id + '"]').attr('class', 'package');

    // un-select child packages if any
    var children = packagesToChildren[id] || [];
    children.forEach(function(p) {
      unSelectPackage(p);
    })
  }

  /**
   * Returns true if the given package can be un-selected.
   *
   * A package can be un-selected if it's not the only one selected in a "single" selection.
   */
  function canUnSelectPackage(id) {
    var parentPackage = packages[packagesToParent[id]];
    if (parentPackage) {
      if (parentPackage.selectionType === 'single') {
        var selectedPackages = [];
        parentPackage.children.forEach(function(p) {
          if (p.selected) {
            selectedPackages.push(p);
          }
        });
        if (selectedPackages.length === 1 && selectedPackages[0].id === id) {
          // only this package selected, cannot un-select
          return false;
        }
      }
    }
    return true;
  }

  // make the initPackagesSelection function accessible from the outside
  nuxeo.initPackagesSelection = initPackagesSelection;
})(nuxeo);
