# About

This addon provides HTML preview for Nuxeo Document. It also integrate a PDF viewer built with [PDF.js](https://github.com/mozilla/pdf.js/), see bellow.

# PDF.js viewer

The PDF.js viewer is built from the [PDF.js](https://github.com/mozilla/pdf.js/) GitHub repository and integrated into `src/main/resources/web/nuxeo.war/viewer`.

The current version is built from this [commit](https://github.com/mozilla/pdf.js/commit/846eb967cc49f5d6ed099a6f10651a8ca68b2692).

## How to update

For now this is done manually as there is no npm / bower package with the full minified PDF.js viewer.

    $ git clone git@github.com:mozilla/pdf.js.git
    $ cd pdf.js

The integrated version is minified with Google Closure Compiler, see https://github.com/mozilla/pdf.js/wiki/Frequently-Asked-Questions#minified.

Update the PDF.js repository on the commit / tag wanted.

Update web/viewer.js to empty the `DEFAULT_URL` variable.

Then build and copy the viewer:

    $ CLOSURE_COMPILER=/usr/local/Cellar/closure-compiler/20150126/libexec/build/compiler.jar node make minified
    $ cp -r build/minified/build build/minified/web /path/to/repo/nuxeo/nuxeo-features/nuxeo-platform-preview/src/main/resources/web/nuxeo.war/viewer/
    $ rm /path/to/repo/nuxeo/nuxeo-features/nuxeo-platform-preview/src/main/resources/web/nuxeo.war/viewer/web/compressed.tracemonkey-pldi-09.pdf
    $ git commit -am "NXP-XXXXX: update PDF.js version"


# Reporting issues

You can follow the developments in the Nuxeo Platform project of our JIRA bug tracker: [https://jira.nuxeo.com/browse/NXP/](https://jira.nuxeo.com/browse/NXP/).

You can report issues on [answers.nuxeo.com](http://answers.nuxeo.com).


# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).
