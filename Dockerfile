# Use the same base image as your GitHub Actions workflow
FROM adoptopenjdk:17-jdk-hotspot

ENV PATH="/scripts:${PATH}"

# Set environment variables
ENV GRADLE_VERSION=7.3.3 \
    GRADLE_HOME=/opt/gradle \
    PATH=$PATH:/opt/gradle/bin

# Install necessary tools
RUN apt-get update \
    && apt-get install -y wget unzip \
    && rm -rf /var/lib/apt/lists/*

# Download and install Gradle
RUN wget -q --show-progress --progress=bar:force:noscroll --no-check-certificate "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -d /opt/gradle "gradle-${GRADLE_VERSION}-bin.zip" \
    && ln -s "${GRADLE_HOME}/bin/gradle" /usr/bin/gradle \
    && rm "gradle-${GRADLE_VERSION}-bin.zip"

# Verify installation
RUN gradle --version

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
