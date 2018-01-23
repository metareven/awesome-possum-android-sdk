[![GitHub license](https://img.shields.io/github/license/telenordigital/awesome-possum.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

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

[PossumCore](core)

[PossumAuth](possumauth)

[PossumGather](possumgather)

License
====================

    Copyright 2017 Telenor Digital AS

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.