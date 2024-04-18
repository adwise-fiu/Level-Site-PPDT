# Use the same base image as your GitHub Actions workflow
FROM gradle:8.7.0-jdk17

ENV PATH="/scripts:${PATH}"
ENV ALIAS="appsec"
ENV CERTIFICATE="ppdt-certificate"

# Verify installation
RUN gradle --version

# Get VIM if I need to debug a bit on container/pod
RUN apt-get update
RUN apt-get install -y vim
RUN apt-get install -y graphviz
RUN vim -h

# Create directories
RUN mkdir /code
RUN mkdir /scripts
RUN mkdir /data

# Copy your project files into the container
COPY . /code

# Move scripts and data to appropriate directories
RUN mv /code/scripts/* /scripts/
RUN mv /code/data/* /data/
RUN chmod +x /scripts/*
WORKDIR /code

# Apparently for Amazon to be happy, I need to import the certificate too, ugh.
# Might as well update openssl certificates for good measure
RUN keytool -import -alias ${ALIAS} -file ${CERTIFICATE} -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt
RUN cp ${CERTIFICATE} /etc/ssl/certs/
RUN update-ca-certificates

# Set permissions
# RUN useradd tree-user
# RUN chown -R tree-user:tree-user /scripts/
# RUN chown -R tree-user:tree-user /code/
# USER tree-user

# Define entrypoint
CMD ["entrypoint.sh"]
