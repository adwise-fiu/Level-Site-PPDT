#!/usr/bin/bash

if [[ -z "${TREE_ROLE}" ]]; then
    ROLE="ALL"
else
    ROLE=${TREE_ROLE}
fi

if [[ $ROLE == "ALL" ]]; then
    echo "Role: All selected"
    gradle -g gradle_user_home test;
elif [[ $ROLE == "SERVER" ]]; then
    echo "Role: Server selected"
    while :; do sleep 5 ; done
    # gradle -g gradle_user_home run -PchooseRole=weka.finito.server_site
elif [[ $ROLE == "SERVER_JOB" ]]; then
    echo "Role: Server selected. The Job version, this was used before, I might remove later."
elif [[ $ROLE == "LEVEL_SITE" ]]; then
    echo "Role: Level-site selected"
    gradle -g gradle_user_home run -PchooseRole=weka.finito.level_site_server
elif [[ $ROLE == "CLIENT" ]]; then
    echo "Role: client"
    while :; do sleep 5 ; done
    # gradle -g gradle_user_home run -PchooseRole=weka.finito.client
else
    echo "Sorry, this is not a valid MPC-PPDT role. Please try again."
    exit
fi
