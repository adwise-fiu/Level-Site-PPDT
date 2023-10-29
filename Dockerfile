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
# https://www.obungi.com/2019/05/08/how-to-create-and-use-self-singed-certificates-in-an-ubuntu-docker-container-to-trust-external-resources/
RUN apt-get update && apt-get install -y curl && apt-get install -y ca-certificates
RUN cp ppdt-certificate /usr/local/share/ca-certificates/
RUN update-ca-certificates

RUN useradd tree-user
RUN chown -R tree-user:tree-user /scripts/
RUN chown -R tree-user:tree-user /code/
USER tree-user

CMD ["entrypoint.sh"]
