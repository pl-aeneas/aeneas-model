# Aeneas Model

Prerequisites
------------
The Aeneas Model requires the [Aeneas Library](https://github.com/pl-aeneas/aeneas), however, it is vendored in the lib folder.

Java/R Interface (JRI) is required to use the model. Please see the [Java/R interface for installation instructions](https://www.rforge.net/JRI/). Once installed, you will need to modify the Makefile and sim.sh script to point the JRI variable to the location of your installation.

Installation
------------

Simply run ```make``` to build the Aeneas model. 


Usage
------------

The sim.sh script bootstraps the JRI and Aeneas libraries to run the model. single.sh contains a running example of how to use sim.sh, and can be executed ``./single.sh``.

The following will run the simulator with 100 interactions using VDBE-5.0. 

```
./sim.sh model.Simulator -runs 1 -interactions 100 -stochastic VBDE_50 -samples 1 -constraint 30.0 -profile 2arm
```

You may run ```./sim.sh model.Simulator``` for additional parameters.

Data
------------
$AENEAS_MODEL_HOME/dump/normal-error-100 contains data from our model along with figures.
