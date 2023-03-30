#!/usr/bin/bash

gradle -g gradle_user_home -v

if [[ -z "${TREE_ROLE}" ]]; then
    ROLE="ALL"
else
    ROLE=${TREE_ROLE}
fi

if [[ $ROLE == "ALL" ]]; then
    gradle -g gradle_user_home assemble;
    gradle -g gradle_user_home test;
elif [[ $ROLE == "SERVER" ]]; then
    gradle -g gradle_user_home assemble;
    gradle -g gradle_user_home server;
elif [[ $ROLE == "LEVEL_SITE" && -n ${LEVEL_SITE} ]]; then
    gradle -g gradle_user_home assemble;
    gradle -g gradle_user_home level ${LEVEL_SITE};
elif [[ $ROLE == "CLIENT" ]]; then
    grade -g gradle_user_home assemble;
    gradle -g gradle_user_home client;
else
    echo "Sorry, this is not a valid MPC-PPDT role. Please try again."
    exit
fi
