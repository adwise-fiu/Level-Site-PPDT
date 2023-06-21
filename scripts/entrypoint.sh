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
    # gradle -g gradle_user_home run -PchooseRole=weka.finito.server_site
elif [[ $ROLE == "LEVEL_SITE" ]]; then
    echo "Role: Level-site selected"
    gradle -g gradle_user_home run -PchooseRole=weka.finito.level_site_server
elif [[ $ROLE == "CLIENT" ]]; then
    echo "Role: client"
    # gradle -g gradle_user_home run -PchooseRole=weka.finito.client
    while :; do sleep 5 ; done
else
    echo "Sorry, this is not a valid MPC-PPDT role. Please try again."
    exit
fi
