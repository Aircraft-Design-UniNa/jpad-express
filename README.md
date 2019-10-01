# JPAD, a Java Application Program Interface for Aircraft Designers (Express release)

JPAD is a Java software library containing classes and utility functions that can be used to build software systems. 

The typical user of JPAD is the aircraft designer, one who is interested in aerodynamic or performance predictions and in parametric studies. The principal focus of the library is the overall aircraft model, conceived as a set of interconnected and parametrized submodels: the wing and its variants (horizontal tail, vertical tail, canard), the fuselage, nacelles, the propulsion system.

## JPAD Main modules

JPAD comes with a set of modules, i.e. interdependent projects. These include:

- `jpad-configs`
- `jpad-core`
- `jpad-cad`
- `jpad-commander`

# How to use JPAD

JPAD has been in development since 2014. The codebase is provided here because we believe in the open source philosophy. Yet, only a selected subset of all functionalities is opensourced and made available in the "Express" release.

__Caveat:__ Some features of JPAD depend upon a couple of native libraries that have been tested only for the Windows 64-bit platform. Hence, we provide instructions for Windows users. A Linux version of JPAD will be available once the library is declared stable and feature complete (release candidate 1.0). 

## Tools

Download and install:

- [Java SE Development Kit 8](http://www.oracle.com/technetwork/pt/java/javase/downloads/jdk8-downloads-2133151.html)
- [Git bash for Windows](https://git-scm.com/downloads)
- [Eclipse IDE](http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/)
- [Gradle](https://gradle.org/)
- e(fx)clipse plugin for EclipseIDE
- Gradle plugin for Eclipse

## Downloading

You can use a Git bash shell to clone the repository. Open the shell, establish a path where you want to download the files, e.g. `C:\Users\John\Dev`. At the shell prompt move to this position and issue the command:

```
git clone https://github.com/Aircraft-Design-UniNa/jpad-express.git
```

if you use HTTPS, or issue the command:

```
git clone git@github.com:Aircraft-Design-UniNa/jpad-express.git
```

if you have set up your shell to use SSH. [See this guide to learn more.](https://help.github.com/articles/which-remote-url-should-i-use/)
Alternatively, you can download the repository as a zip archive [from this link.](https://github.com/Aircraft-Design-UniNa/jpad/archive/master.zip)

Once you have cloned successfully the JPAD repository, e.g. from `C:\Users\John\Dev`, you end up with the subfolder `jpad-express` populated with all the necessary files to use the library. We call this folder `<JPAD_ROOT>`.

## Importing the projects into Eclipse

Open Eclipse and prepare to import a couple of projects that live in `<JPAD_ROOT>`.

### Import existing Projects into Workspace

From the menu `File -> Import` select the import wizard `General -> Existing Projects into Workspace`. Click `Next`. Chose `Select root directory` and browse to `<JPAD_ROOT>`. The *japa-express* project will be recognized by the wizard. Select the project and click `Finish`.

### Running the example programs

To run the test programs in Eclipse go to the menu `Run -> Run Configurations`. Explore the dialog that manages all the existing run configurations, which are stored as `.launch` files. Select a configuration from the left pane (in the Java Application group). 

Before running a configuration do check the panels on the right pane. Explore the `Main`, `Arguments` and the `Environment` tabs. In the `Environment` tab make sure you edit the `Path` variable, which expands the `JPAD_ROOT` variable (see the string `${JPAD_ROOT}`). Select the `Path` variable, click `Edit`, click `Variables`, click `Edit Variables`, finally create the new variable `JPAD_ROOT` pointing to the path `<JPAD_ROOT>`.

---
[DAF Research Group at University Naples Federico II](http://www.daf.unina.it/)

**Design of Aircraft and Flight technologies**

<img src="https://github.com/Aircraft-Design-UniNa/jpad/wiki/images/Logo_DAF_Flat-Elevator.png" width="400"/>
