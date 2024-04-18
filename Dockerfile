# Use the same base image as your GitHub Actions workflow
FROM gradle:8.7.0-jdk17

ENV PATH="/scripts:${PATH}"

# Verify installation
RUN gradle --version

RUN apt-get update
RUN apt-get install -y vim
RUN apt-get install -y graphviz

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

RUN useradd tree-user
# Set permissions
RUN chown -R tree-user:tree-user /scripts/
RUN chown -R tree-user:tree-user /code/
USER tree-user

# Define entrypoint
CMD ["entrypoint.sh"]
