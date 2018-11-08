# PCJ

PCJ is Java library for parallel computing in Java. It is based on the PGAS (Partitioned Global Address Space) paradigm. It allows for easy implementation in Java of any parallel algorithm. PCJ application can be run on laptop, workstation, cluster and HPC system including large supercomputers. It has been demonstrated that PCJ applications scale up to 200&nbsp;000 cores. Examples and more information at <http://pcj.icm.edu.pl>.

Library requires at least Java 8, no modifications to Java syntax or JVM are neccessary.

## Using PCJ Library

PCJ Library is now available on Maven Central Repository.

For maven project, just add this dependency to your `pom.xml` file.

    <dependency>
        <groupId>pl.edu.icm.pcj</groupId>
        <artifactId>pcj</artifactId>
        <version>5.0.6</version>
    </dependency>

If you are using gradle, add those lines to your `build.gradle` file:

    implementation 'pl.edu.icm.pcj:pcj:5.0.6'
    annotationProcessor 'pl.edu.icm.pcj:pcj:5.0.6'


## Building project

If you wish to compile project by your own, use these instructions:

* to package the jar: ```./gradlew assemble```    or  ```gradlew.bat assemble```
* to create javadoc: ```./gradlew javadoc``` or ```gradlew.bat javadoc```


## Importing the project in eclipse

Execute `./gradlew eclipse`, start `eclipse`, and use
`File -> Import : Existing Projects into Workspace`.  See
<http://gradle.org/docs/current/userguide/eclipse_plugin.html> for more
information.


## Reference
The usage should be acknowledged by reference to the [PCJ web site](http://pcj.icm.edu.pl) and/or reference to the papers:
* M. Nowicki, Ł. Górski, P. Bała. [PCJ – Java Library for Highly Scalable HPC and Big Data](https://ieeexplore.ieee.org/abstract/document/8514322) Processing 2018 International Conference on High Performance Computing \& Simulation (HPCS), pp:12-20. IEEE, 2018
* Marek Nowicki, Magdalena Ryczkowska, Łukasz Górski, Michał Szynkiewicz, Piotr Bała. ["PCJ - a Java library for heterogenous parallel computing"](http://www.wseas.us/e-library/conferences/2016/barcelona/SECEA/SECEA-08.pdf) In: X. Zhuang (Ed.) Recent Advances in Information Science (Recent Advances in Computer Engineering Series vol 36) WSEAS Press 2016 pp. 66-72
* Marek Nowicki, Łukasz Górski, Patryk Grabarczyk, Piotr Bała. ["PCJ - Java library for high performance computing in PGAS model"](https://ieeexplore.ieee.org/abstract/document/6903687/) In: W. W. Smari and V. Zeljkovic (Eds.) 2012 International Conference on High Performance Computing and Simulation (HPCS) IEEE 2014 pp. 202-209
* Marek Nowicki, Piotr Bała. ["PCJ - New Approach for Parallel Computations in Java"](https://link.springer.com/chapter/10.1007/978-3-642-36803-5_8) In: P. Manninen, P. Oster (Eds.) Applied Parallel and Scientific Computing, LNCS 7782, Springer, Heidelberg (2013) pp. 115-125
* Marek Nowicki, Piotr Bała. ["Parallel computations in Java with PCJ library"](https://ieeexplore.ieee.org/abstract/document/6266941/) In: W. W. Smari and V. Zeljkovic (Eds.) 2012 International Conference on High Performance Computing and Simulation (HPCS) IEEE 2012 pp. 381-387
