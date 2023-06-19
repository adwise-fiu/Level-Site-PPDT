# MPC-PPDT
[![Build Gradle project](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml/badge.svg)](https://github.com/AndrewQuijano/MPC-PPDT/actions/workflows/build-gradle-project.yml)  
Implementation of the PPDT in the paper "Privacy Preserving Decision Trees in a Multi-Party Setting: a Level-Based Approach"

## Libraries
* crypto.jar library is from this [repository](https://github.com/AndrewQuijano/Homomorphic_Encryption)
* weka.jar library is from [SourceForge](https://sourceforge.net/projects/weka/files/weka-3-9/3.9.5/),
  download the ZIP file and import the weka.jar file**

** To be confirmed/tested again...

## Installation
It is a requirement to install [SDK](https://sdkman.io/install) to install Gradle.
You need to install the following packages, to ensure everything works as expected
```bash
sudo apt-get install -y default-jdk, default-jre, graphviz, curl, python3-pip
pip3 install pyyaml
pip3 install configobj
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle
```

Run this command and all future commands from `the repository root`, run the following command once to install docker.
**Reboot your machine, then re-run the command to install minikube.**
```bash
bash setup.sh
```

## Usage

### Running it locally

1. Check the `config.properties` file is set to your needs. Currently:
   1. It assumes level-site 0 would use port 9000, level-site 1 would use port 9001, etc.
      1. If you modify this, provide a comma separated string of all the ports for each level-site.
      2. Currently, it assumes ports 9000 - 9009 will be used.
   2. key_size corresponds to the key size of both DGK and Paillier keys.
   3. precision controls how accurate to measure thresholds that are decimals. If a value was 100.1, then a precision of
      1 would set this value to 1001.
   4. The data would point to the directory with the `answer.csv` file and all the training and testing data.
2. Currently, the [test file](src/test/java/PrivacyTest.java) will read from the `data/answers.csv` file.
   1. The first column is the training data set,
      it is required to be a .arff file to be compatible with Weka.
      Alternatively, you can pass a .model file, which is a pre-trained Weka model.
      It is assumed this is a J48 classifier tree model.
   2. The second column would the name of an input file that is tab separated with the feature name and value
   3. The third column would be the expected classification given the input from the second column.
      If there is a mismatch, there will be an assert error.

To run the end-to-end test, run the following:
```bash
sh gradlew build
```

When the testing is done, you will have an output directory containing both the DT model and a text file on how to draw
your tree. Input the contents of the text file into the website [here](https://dreampuf.github.io/GraphvizOnline/) to get a
drawing of what the DT looks like.

### Running on local host Kubernetes Cluster
To make it easier for deploying on the cloud, we also provided a method to export our system into Kubernetes.
This would assume one execution rather than multiple executions.

#### Set Training and testing files
First, you need to edit the environment variables:
1. In the `client_deployment.yaml` file, you need to change the value of `VALUES` to point to the input vector to evaluate
2. In the `server_site_deployment.yaml` file, you need to change the value of the `TRAINING` to point to the file with the training data.

*To be updated with converting to jobs*
#### Creating a Kubernetes Secret
You should set up a Kubernetes secret file, called `ppdt-secrets.yaml` in the `k8/level-sites` folder.
In the yaml file, you will need to replace <SECRET_VALUE> with a random string encoded in Base64.
This secret is used in the AES encryption between level sites.
```yaml
apiVersion: v1
kind: Secret
metadata:
    name: ppdt-secrets
type: Opaque
data:
  aes-key: <SECRET_VALUE>>
```

or you can use the command:

    kubectl create secret generic ppdt-secrets --from-literal=aes-key=<SECRET_VALUE>

#### Using Minikube
You will need to start and configure minikube. When writing the paper, we provided 8 CPUs and 20 GB of memory,
but feel free to modify the arguments that fit your computer's specs.

    minikube start --cpus 8 --memory 20000
    eval $(minikube docker-env)

#### Running Kubernetes Commands
After starting minikube you will need to build the necessary Docker image using
the docker build command. The resulting image needs to have a specific label,
ppdt:experiment. You can build this image using the following command.

    docker build -t ppdt:experiment .

The next step is to deploy the level sites. The level sites need to be deployed
before any other portion of the system. This can be done by using the following
command.

    kubectl apply -f k8/level_sites

You will then need to wait until all the level sites are launched. To verify
this, please run the following command. All the pods that say level_site should have a status _running_.

    kubectl get pods

The output of `kubectl get pods` would look something like:
```
NAME                                         READY   STATUS      RESTARTS        AGE
ppdt-level-site-01-deploy-7dbf5b4cdd-wz6q7   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-02-deploy-69bb8fd5c6-wjjbs   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-03-deploy-74f7d95768-r6tn8   1/1     Running     1 (16h ago)     16h
ppdt-level-site-04-deploy-6d99df8d7b-d6qlj   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-05-deploy-855b649896-82hlm   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-06-deploy-6578fc8c9b-ntzhn   1/1     Running     1 (16h ago)     16h
ppdt-level-site-07-deploy-6f57496cdd-hlggh   1/1     Running     1 (16h ago)     16h
ppdt-level-site-08-deploy-6d596967b8-mh9hz   1/1     Running     1 (2m39s ago)   16h
ppdt-level-site-09-deploy-8555c56976-752pn   1/1     Running     1 (16h ago)     16h
ppdt-level-site-10-deploy-67b7c5689b-rkl6r   1/1     Running     1 (2m39s ago)   16h
```

It does take time for the level-site to be able to accept connections. Run the following command on a level-site,
and wait for an output in standard output saying `Ready to accept connections`. Set `<LEVEL-SITE-POD-NAME>`
to one of the pod names from the output, e. g. `ppdt-level-site-01-deploy-7dbf5b4cdd-wz6q7`.

    kubectl logs -f <LEVEL-SITE-POD-NAME>

After verifying that all the pods are running properly, the next step is to
start the server site. To do this, run the following command.

    kubectl apply -f k8/server_site

To verify that the server site is finished running, use the following commands to confirm the server_site is _running_
and check the logs to confirm we see `Training Successful` for all the level-sites.

    kubectl get pods
    kubectl logs -f <SERVER-SITE-POD-NAME>

After the server site has completed successfully we are ready to run the client.
To run the client, simply run the following command.

    kubectl apply -f k8/client

To get results, all you need to do is print the stdout of each of the level_sites
and from the client. To do this, first get all the pods.

    kubectl get pods

Then, for all level_sites and clients you can get the printout of stdout by
using the logs command for each pod.

    kubectl logs <POD-NAME> 

#### Clean up

If you want to re-build everything in the experiment, run the following

    docker system prune --force
    minikube delete

### Running it on an EKS Cluster

#### Installation
1. First install [eksctl](https://eksctl.io/introduction/#installation)

2. Create a user. Using Access analyzer, the customer inline policy needed is listed here:
* still undergoing more testing
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "iam:GetRole",
                "ec2:AuthorizeSecurityGroupIngress",
                "iam:CreateRole",
                "iam:DeleteRole",
                "cloudformation:*",
                "ec2:RunInstances",
                "iam:AttachRolePolicy",
                "iam:PutRolePolicy",
                "ec2:DescribeSecurityGroups",
                "ec2:AssociateRouteTable",
                "iam:DetachRolePolicy",
                "ec2:CreateLaunchTemplate",
                "ec2:DescribeInstanceTypeOfferings",
                "iam:DeleteRolePolicy",
                "iam:ListAttachedRolePolicies",
                "ec2:DescribeVpcs",
                "ec2:CreateRoute",
                "iam:GetOpenIDConnectProvider",
                "ec2:DescribeSubnets",
                "ec2:DescribeKeyPairs",
                "iam:GetRolePolicy"
            ],
            "Resource": "*"
        }
    ]
}
```

3. Obtain AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY of the user account. [See the documentation provided here](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)

4. run `aws configure` to input the access id and credential.

5. Run the following command to create the cluster
```bash
eksctl create cluster --config-file eks-config/config.yaml
```

5. Confirm the EKS cluster exists using the following
```bash
eksctl get clusters --region us-east-2
```

#### Running the experiment
1. Once you confirm the cluster is created, you need to register the cluster with kubectl:
```bash
aws eks update-kubeconfig --name ppdt --region us-east-2
```

2. Run the same commands as shown in [here](#running-kubernetes-commands)

3. Obtain the results of the classification using `kubectl logs` to the pods deployed on EKS.

#### Clean up
Destroy the EKS cluster using the following:
```bash
eksctl delete cluster --config-file eks-config/config.yaml --wait
docker system prune --force
```

## Authors and Acknowledgement
Code Authors: Andrew Quijano, Spyros T. Halkidis, Kevin Gallagher

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Project status
Fully tested and completed. Future work currently includes:
* Converting Server-site and client to Kubernetes jobs instead
* See if I can run this on AWS EKS too