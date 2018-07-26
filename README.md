PCJ
===

PCJ is Java library for parallel computing in Java. It is based on the PGAS (Partitioned Global Address Space) paradigm. It allows for easy implementation in Java of any parallel algorithm. PCJ application can be run on laptop, workstation, cluster and HPC system including large supercomputers. It has been demonstrated that PCJ applications scale up to 200 000 cores. Examples and more information at <http://pcj.icm.edu.pl>.

Library requires at least Java 8, no modifications to Java syntax or JVM are neccessary.

Building project
----------------

* to package the jar: ```./gradlew assemble```    or  ```gradlew.bat assemble```
* to create javadoc: ```./gradlew javadoc``` or ```gradlew.bat javadoc```

Importing the project in eclipse
--------------------------------

Execute `./gradlew eclipse`, start `eclipse`, and use
`File -> Import : Existing Projects into Workspace`.  See
<http://gradle.org/docs/current/userguide/eclipse_plugin.html> for more
information.
