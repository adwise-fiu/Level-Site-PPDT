#!/bin/bash
#================================================================================
#title           : setup.sh
#description     : This script will install the software necessary to complete
#		    Run the Kubernetes cluster for Level-Site-PPDT
#			This is based on a script by John Ryan Allen for an Application Security Assignment
#author		: Andrew Quijano (afq2003@nyu.edu)
#date            : April 18, 2023
#version         : 0.1
#usage		 : sudo bash setup.sh
#notes           : Run as standard user, ***NOT ROOT***. Provide sudo password
#		    when prompted. Tested on fresh install of Ubuntu 20.04.3 LTS.
#		    Must run this script twice to complete the installation.
#================================================================================

# Install docker if user is not already in docker group
if [[ $(id) != *\(docker\)* ]]; then
	# INSTALL DOCKER
	# https://docs.docker.com/engine/install/ubuntu/
	echo '##################################################'
	echo '[*] Installing Docker...'
	echo '##################################################'
	sleep 3

  for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done

  # Add Docker's official GPG key:
  sudo apt-get update
  sudo apt-get install ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc

  # Add the repository to Apt sources:
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get update

  sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

	# CONFIGURE STANDARD USER TO MANAGE DOCKER WITHOUT ROOT
	echo '##################################################'
	echo '[*] Configuring Docker...'
	echo '##################################################'
	sleep 3
	sudo usermod -aG docker "$USER"

	echo '#####################################################################'
	echo '[*] Almost there! Reboot and run this script one more time to finish.'
	echo '#####################################################################'
else
	# INSTALL KUBERNETES CLI TOOLS
	# https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/
	echo '##################################################'
	echo '[*] Installing kubectl...'
	echo '##################################################'
	sleep 3
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
  rm kubectl

  # https://minikube.sigs.k8s.io/docs/start/
	echo '##################################################'
	echo '[*] Installing minikube...'
	echo '##################################################'
	curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
	sudo install minikube-linux-amd64 /usr/local/bin/minikube
	rm minikube-linux-amd64
fi