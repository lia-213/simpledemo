FROM maven:3.9.9-amazoncorretto-21-debian AS build
# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml .
COPY src ./src
# Build the application using Maven
RUN mvn clean package -DskipTests

RUN mvn -f pom.xml -q clean package -DskipTests

EXPOSE 8080

FROM maven:3.9.9-amazoncorretto-21-debian
WORKDIR /app

# Install Python and bash so we can run both Java and Python in the final image.
# Install Python and bash so we can run both Java and Python in the final image.
RUN apt-get update \
	&& apt-get install -y --no-install-recommends python3 python3-pip bash \
	&& rm -rf /var/lib/apt/lists/*

# Copy built Java artifact from build stage and Python app from context
COPY --from=build /app/target/ilp-0.0.1-SNAPSHOT.jar ./app.jar
COPY cw3_command_center/requirements.txt ./requirements.txt
COPY cw3_command_center/ ./python/

# Install Python dependencies in final image (use explicit python -m pip)
# Add --break-system-packages for Debian 12+ PEP 668 protection
RUN python3 -m pip install --break-system-packages --upgrade pip setuptools wheel && \
	python3 -m pip install --break-system-packages --no-cache-dir -r requirements.txt

# Add entrypoint script that will start both processes and exit when either stops
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

EXPOSE 8080
EXPOSE 8501
EXPOSE 8000

ENTRYPOINT ["/bin/bash","-c","./entrypoint.sh"]
