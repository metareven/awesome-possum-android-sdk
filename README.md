#Awesome Possum

This library is a complete identification and authentication neural network implementation. The 
project is split into three parts:

    PossumCore - handles the data collection and defines the different detectors
    |
    |-- PossumAuth - handles authentication with the data collected from core
    |
    |-- PossumGather - handles upload of data to cloud from core
    
This temporary project can be used to get some insight into the workings of it, the library will
soon be split into three different repos used in combination.

A link to the specific parts of the repo can be found here:

[PossumCore](core/README.md)

[PossumAuth](possumauth/README.md)

[PossumGather](possumgather/README.md)