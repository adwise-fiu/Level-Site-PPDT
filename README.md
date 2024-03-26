# Level-Site-PPDT
[![Build Gradle project](https://github.com/adwise-fiu/Level-Site-PPDT/actions/workflows/build-gradle-project.yml/badge.svg)](https://github.com/AndrewQuijano/Level-Site-PPDT/actions/workflows/build-gradle-project.yml)  
[![codecov](https://codecov.io/gh/adwise-fiu/Level-Site-PPDT/branch/main/graph/badge.svg?token=eEtEvBZYu9)](https://codecov.io/gh/AndrewQuijano/Level-Site-PPDT)  
Implementation of the PPDT in the paper "Evaluating Outsourced Decision Trees by a Level-Based Approach"

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
rm kubeseal

# Install Helm
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
rm get_helm

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
eksctl create cluster --config-file eks-config/config.yaml
```

- Confirm the EKS cluster exists using the following
```bash
eksctl get clusters --region us-east-2
```

- Once you confirm the cluster is created, you need to register the cluster with kubectl:
```bash
aws eks update-kubeconfig --name ppdt --region us-east-2
```

### Using/Creating a Kubernetes Sealed Secret
It is suggested you use the existing sealed secret. The password in this secret is aligned with what is on the keystore,

```commandline
kubectl apply -f ppdt-sealedsecret.yaml
```

Alternatively, you can create a new sealed secret as follows:
```bash
kubectl create secret generic ppdt-secrets  --from-literal=keystore-pass=<SECRET_VALUE>
kubectl get secret ppdt-secrets -o yaml | kubeseal > ppdt-sealedsecret.yaml
```
However, if you make a new sealed secret, you should re-make the keystore as well.

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
The next step is to start the server site. To do this, run the following command.

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-server-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.server --args <TRAINING-FILE>"

It does take time for the level-site to be able to accept connections. Run the following command on the first level-site,
and wait for an output in standard output saying `Ready to accept connections at: 9000`. Use CTRL+C to exit the pod.

    kubectl logs -f $(kubectl get pod -l "pod=ppdt-level-site-01-deploy" -o name)


To verify that the server site is ready, use the following command to confirm the server_site is _running_
and check the logs to confirm we see `Server ready to get public keys from client-site`.

    kubectl logs -f $(kubectl get pod -l "pod=ppdt-server-deploy" -o name)

**In a NEW terminal**, start the client, run the following commands to complete an evaluation. 
You would point values to something like `/data/hypothyroid.values`

    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE>"

    # Test WITHOUT level-sites
    kubectl exec -i -t $(kubectl get pod -l "pod=ppdt-client-deploy" -o name) -- bash -c "gradle run -PchooseRole=weka.finito.client --args <VALUES-FILE> --server"

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
eksctl delete cluster --config-file eks-config/config.yaml --wait
docker system prune --force
```

Destroy the MiniKube environment as follows:
```bash
minikube delete
docker system prune --force
```

## Authors and Acknowledgement
Code Authors: Andrew Quijano, Spyros T. Halkidis, Kevin Gallagher

## License
[MIT](https://choosealicense.com/licenses/mit/)

## Project status
Fully tested and completed. Although I believe I need a label encoder to compare two strings.
