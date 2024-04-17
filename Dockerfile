# Use the same base image as your GitHub Actions workflow
FROM adoptopenjdk:17-jdk-hotspot

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

# Set up workspace directory
WORKDIR /code

# Copy your project files into the container
COPY . /code

# Create directories
RUN mkdir /scripts
RUN mkdir /data

# Move scripts and data to appropriate directories
RUN mv /code/scripts/* /scripts/
RUN mv /code/data/* /data/

# Set permissions
RUN chmod +x /scripts/*

# Define entrypoint
CMD ["entrypoint.sh"]
