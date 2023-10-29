FROM gradle:latest

ENV PATH="/scripts:${PATH}"

RUN mkdir /code
RUN mkdir /scripts
RUN mkdir /data

ADD . /code/

RUN mv /code/scripts/* /scripts/
RUN mv /code/data/* /data/
RUN chmod +x /scripts/*
WORKDIR /code

# Move certificate
# https://stackoverflow.com/questions/54402673/how-to-fix-ssl-certificate-problem-self-signed-certificate-in-certificate-chain
# https://stackoverflow.com/questions/26028971/docker-container-ssl-certificates
RUN mv ppdt-certificate /etc/ssl/certs/
RUN update-ca-certificates

RUN useradd tree-user
RUN chown -R tree-user:tree-user /scripts/
RUN chown -R tree-user:tree-user /code/
USER tree-user

CMD ["entrypoint.sh"]
