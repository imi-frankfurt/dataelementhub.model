# DataElementHub Model

DataElementHub Model is based on the fundamental concepts of the [Samply.MDR](https://bitbucket.org/medicalinformatics/mig.samply.mdr.gui).
Based on the experience gained, the concept was revised and redeveloped, with particular emphasis on
the separation of the backend and frontend.
DataElementHub Model is the next evolution of [MIG.MDR.model](https://github.com/mig-frankfurt/mdr.model), introducing
a rebranding.

Please see the [DataElementHub](https://dataelementhub.de/) website for further information.

DataElementHub model serves as a layer between dataelementhub.dal and dataelementhub.rest.

## Build

Use maven to build the `jar` file:

```
mvn clean package
```