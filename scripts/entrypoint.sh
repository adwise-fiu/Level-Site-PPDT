#!/usr/bin/bash

if [[ -z "${TREE_ROLE}" ]]; then
    ROLE="ALL"
else
    ROLE=${TREE_ROLE}
fi

gradle -g gradle_user_home assemble
if [[ $ROLE == "ALL" ]]; then
    gradle -g gradle_user_home test;
elif [[ $ROLE == "SERVER" ]]; then
    gradle -g gradle_user_home run -PchooseRole=weka.finito.server_site
elif [[ $ROLE == "LEVEL_SITE" ]]; then #&& -n ${LEVEL_SITE} ]]; then
    gradle -g gradle_user_home run -PchooseRole=weka.finito.level_site_server
elif [[ $ROLE == "CLIENT" ]]; then
    gradle -g gradle_user_home run -PchooseRole=weka.finito.client
else
    echo "Sorry, this is not a valid MPC-PPDT role. Please try again."
    exit
fi
