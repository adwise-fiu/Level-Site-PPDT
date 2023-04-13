# MPC-PPDT
[![Build Gradle project](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml/badge.svg)](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml)  
Implementation of the PPDT in the paper "Privacy Preserving Decision Trees in a Multi-Party Setting: a Level-Based Approach"

## Libraries
* crypto.jar library is from this [repository](https://github.com/AndrewQuijano/Homomorphic_Encryption)
* weka.jar library is from [SourceForge](https://sourceforge.net/projects/weka/files/weka-3-9/3.9.5/), 
download the ZIP file and import the weka.jar file**

** To be confirmed/tested again...

## Installation

## Usage

#### With Minikube
In order to use this project with Kubernetes locally using minikube, please
checkout the k8s branch using the following command.

    git checkout k8s

Then you will need to start and configure minikube.

    minikube start
    eval $(minikube docker-env)

After starting minikube you will need to build the necessary Docker image using
the docker build command. The resulting image needs to have a specific label,
ppdt:experiment. You can build this image using the following command.

    docker build -t ppdt:experiment .

After the docker container is built you will need to add the k8s secret that the
level_sites are expecting. This secret is used in the AES encryption between
level sites. To add the secret, use the following command.

    kubectl create secret generic ppdt-secrets --from-literal=aes-key=<SECRET_VALUE>

In the command above, you will need to replace <SECRET_VALUE> with a random
string. 

The next step is to deploy the level sites. The level sites need to be deployed
before any other portion of the system. This can be done by using the following
command.

    kubectl apply -f k8/level_sites

You will then need to wait until all of the level sites are launched. To verify
this, please run the following command.

    kubectl get pods

All of the pods that say level_site should have a status _running_.

After verifying that all of the pods are running properly, the next step is to
start the server site. To do this, run the following command.

    kubectl apply -f k8/server_site

Then, when it's finished working properly we are ready to launch the client. To
verify that the server site is finished running, use the following command and
wait until the status of the server site pod says _completed_.

    kubectl get pods

After the server site has completed successfully we are ready to run the client.
To run the client, simply run the following command.

    kubectl apply -f k8/client

TODO: WRITE SECTION FOR GETTING RESULTS.

## Authors and Acknowledgement
Code Authors: Andrew Quijano, Spyros T. Halkidis, Kevin Gallagher

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Project status
Fully tested and completed
