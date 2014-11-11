To regenerate sources type "ant" and make sure that:
- JFlex.jar (JFlex 1.4.3) is present in $ANT/lib
- java-cup.jar (CUP v0.10k) is present in $ANT/lib

Also java-cup has to be AFTER JFLex in the internal ANT classpath, otherwise you get some
java.lang.NoSuchMethodError: java_cup.runtime.lr_parser.getSymbolFactory()Ljava_cup/runtime/SymbolFactory
due to some other java_cup version being fetched from somewhere (maybe Xalan) when JFlex
depends on a copy if some java-cup classes...

To control classpath order you may want to rename java-cup.jar into ZZZjava-cup.jar

Note also that the Nuxeo Maven repository has a java-cup-0.11a.jar which is actually 0.10k.
