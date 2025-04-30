# Level-Site-PPDT
[![Build Gradle project](https://github.com/adwise-fiu/Level-Site-PPDT/actions/workflows/build-gradle-project.yml/badge.svg)](https://github.com/AndrewQuijano/Level-Site-PPDT/actions/workflows/build-gradle-project.yml)  
[![codecov](https://codecov.io/gh/adwise-fiu/Level-Site-PPDT/branch/main/graph/badge.svg?token=eEtEvBZYu9)](https://codecov.io/gh/AndrewQuijano/Level-Site-PPDT)  
Implementation of the PPDT in the paper "Evaluating Outsourced Decision Trees by Level-Based Approach"

## Libraries
* crypto.jar library is from this [repository](https://github.com/adwise-fiu/Homomorphic_Encryption)
* weka.jar library is from [SourceForge](https://sourceforge.net/projects/weka/files/weka-3-9/3.9.5/),
  download the ZIP file and import the weka.jar file

## Installation
It is a requirement to install [SDK](https://sdkman.io/install) to install Gradle.
You need to install the following packages, to ensure everything works as expected
```bash
sudo apt-get install -y default-jdk, default-jre, graphviz, curl, python3-pip
pip3 install pyyaml
pip3 install configobj
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
# In a new terminal, you run this command
sdk install gradle
```

Run this command and all future commands from `Level-Site-PPDT` folder, run the following command once to install docker and MiniKube.

**Reboot your machine, then re-run the command to install minikube.**
```bash
bash setup.sh
```

Also,
remember to install [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets?tab=readme-ov-file#installation).
```bash
sudo apt-get install jq

# Fetch the latest sealed-secrets version using GitHub API
KUBESEAL_VERSION=$(curl -s https://api.github.com/repos/bitnami-labs/sealed-secrets/tags | jq -r '.[0].name' | cut -c 2-)

# Check if the version was fetched successfully
if [ -z "$KUBESEAL_VERSION" ]; then
    echo "Failed to fetch the latest KUBESEAL_VERSION"
    exit 1
fi

wget "https://github.com/bitnami-labs/sealed-secrets/releases/download/v${KUBESEAL_VERSION}/kubeseal-${KUBESEAL_VERSION}-linux-amd64.tar.gz"
tar -xvzf kubeseal-"${KUBESEAL_VERSION}"-linux-amd64.tar.gz kubeseal
sudo install -m 755 kubeseal /usr/local/bin/kubeseal
rm kubeseal*

# Install Helm
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
rm ./get_helm.sh

# Add Sealed Secret Cluster
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm install sealed-secrets -n kube-system --set-string fullnameOverride=sealed-secrets-controller sealed-secrets/sealed-secrets
```

Before you run the PPDT, make sure to create your keystore, this is necessary as the level-sites use TLS sockets. 
Either run `create_keystore.sh` script, make sure the password is consistent with the Kubernetes secret, or just use the Sealed Secret.

## Running PPDT locally

1. Check the `config.properties` file is set to your needs. Currently:
   1. It assumes level-site 0 would use port 9000, level-site 1 would use port 9001, etc.
      1. If you modify this, provide a comma-separated string of all the ports for each level-site.
      2. Currently, it assumes ports 9000–9009 will be used.
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
      If there is a mismatch, there will be an assertion error.

To run the end-to-end test, run the following:
```bash
sh gradlew build
```

When the testing is done, you will have an output directory containing both the DT model and a text file on how to draw
your tree. Input the contents of the text file into the website [here](https://dreampuf.github.io/GraphvizOnline/) to get a
drawing of what the DT looks like.

If you want to analyze the level of each classification in a pre-trained decision tree from the `data` folder, 
run the following (where argument is the name of the dataset):
```bash
./gradlew run -PchooseRole=weka.finito.utils.depth_analysis --args spambase
```
This will read the DT in `data/spambase.model` which was trained from the data set `data/spambase.arff`. 
It will classify all the data in the training set,
and get the level (1, ..., d) of the classification within the DT model. 
In the paper, I used this to argue that assuming most training data is like testing data,
you likely will never need to go down the whole tree often.

## Running PPDT on Kubernetes clusters
To make it easier for deploying on the cloud, we also provided a method to export our system into Kubernetes.
This would assume one execution rather than multiple executions.

### Option 1 - Using Minikube
You will need to start and configure minikube. When writing the paper, we provided 8 CPUs and 20 GB of memory; this was set using the arguments that fit your computer's specs.

    minikube start --cpus 8 --memory 20000
    eval $(minikube docker-env)

### Option 2- Running it on an EKS Cluster

- First install [eksctl](https://eksctl.io/installation/?h=install)

- Create a user with sufficient permissions. Go to IAM, Select Users, Create User, Attach Policies directly, for a quick experiment select all permission.

- Obtain AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY of the user account. [See the documentation provided here](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html)

- run `aws configure` to input the access id and credential.

- Run the following command to create the cluster
```bash
eksctl create cluster --config-file eks-config/single-cluster.yaml
```

- Confirm the EKS cluster exists using the following
```bash
eksctl get clusters --region us-east-1
```

- Once you confirm the cluster is created, you need to register the cluster with kubectl:
```bash
aws eks update-kubeconfig --name ppdt --region us-east-1
```

### Using/Creating a Kubernetes Sealed Secret
It is suggested you use the existing sealed secret. The password in this secret is aligned with what is on the keystore.

```commandline
kubectl apply -f ppdt-sealedsecret.yaml
```

Alternatively, you can create a new sealed secret as follows:
```bash
kubectl create secret generic ppdt-secrets --from-literal=keystore-pass=<SECRET_VALUE>
kubectl get secret ppdt-secrets -o yaml | kubeseal --scope cluster-wide > ppdt-sealedsecret.yaml
```
However, if you make a new sealed secret, you should re-make the keystore as well. Just remember, sealed secrets do not work in multiple clusters by default, as a heads-up.

### Running Kubernetes Commands
The next step is to start deploying all the components running the following:

    kubectl apply -f k8/server
    kubectl apply -f k8/level_sites
    kubectl apply -f k8/client

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
It does take time for the level-site to be able to accept connections. Run the following command on the first level-site,
and wait for an output in standard output saying `LEVEL SITE SERVER STARTED!`. Use CTRL+C to exit the pod.

    kubectl logs -f $(kubectl get pod -l "pod=ppdt-level-site-01-deploy" -o name)
    kubectl logs -f $(kubectl get pod -l "pod=ppdt-level-site-10-deploy" -o name)

Next, you need to run the server to create Decision Tree and split the model among the level-sites. 
You can run it either connecting via a terminal to the pod using the commands below.

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-server-deploy" -o name) -- /bin/bash
    gradle run -PchooseRole=weka.finito.server --args <TRAINING-FILE>

Alternatively, you can combine the above commands as follows:

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-server-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.server --args <TRAINING-FILE>"

Once you see this output `Server ready to get public keys from client-site`, you need to run the client.

**In a NEW terminal**, start the client, run the following commands to complete an evaluation. 
You would point values to something like `/data/hypothyroid.values`.

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- /bin/bash
    gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>
    
    # Test WITHOUT level-sites
    gradle run -PchooseRole=weka.finito.client --args '<VALUES-FILE> --server'

Alternatively, you can combine both commands in one go as follows:

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"

    # Test WITHOUT level-sites
    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args '<VALUES-FILE> --server'"

### Re-running with different experiments
If you are just re-running the client with the same or different values file, just re-run the above command again. 
However, if you want to test with another data set, best to just rebuild the environment by deleting everything first.

```bash
kubectl delete -f k8/client
kubectl delete -f k8/server
kubectl delete -f k8/level_sites
```

Then repeat the instructions on the previous section.

### Clean up
Destroy the EKS cluster using the following:
```bash
eksctl delete cluster --config-file eks-config/single-cluster.yaml --wait
```

Destroy the MiniKube environment as follows:
```bash
minikube delete
```

## Authors and Acknowledgement
Code Authors: Andrew Quijano, Spyros T. Halkidis, Kevin Gallagher

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Project status
The project is fully tested. 

## Current Issues
1. Not sure why the encryption library seems to have a bug in some specific comparisons in spambase and hypothyroid. I will debug these soon, but overall this works like a charm.
2. TLS Sockets do not work on EKS, but I will fix this eventually. It works on all connections except once level-site 1 reaches out to the client for evaluation.
3. Much bigger issue, so the first few runs of this application on EKS, the comparisons are pretty fast, like it takes about 0.5 seconds. But after like 10+ comparisons, the comparison performance just drops off a cliff to like 1 second. 
The only way I see to restore the same level of performance is to rebuild the EKS cluster. I have NO idea why this performance drop occurs, and I have tried deleting and rebuilding the pods, and even restarting the EC2 instances.

