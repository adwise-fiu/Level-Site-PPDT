FROM gradle:latest

ENV PATH="/scripts:${PATH}"

RUN mkdir /code
RUN mkdir /scripts

ADD . /code/

RUN mv /code/scripts/* /scripts/
RUN chmod +x /scripts/*
WORKDIR /code

RUN useradd tree-user
RUN chown -R tree-user:tree-user /scripts/
RUN chown -R tree-user:tree-user /code/
USER tree-user

CMD ["entrypoint.sh"]
