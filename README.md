# OpenLibrary Elasticsearch Indexer

## About Open Library & Data Dumps

[Open Library](https://openlibrary.org/) is an initiative to create a universal digital library. 
It provides access to a vast collection of books, metadata, and related information. 
Open Library offers [publicly available data dumps](https://openlibrary.org/developers/dumps) that contain structured bibliographic records in JSON format.

You can download these data dumps from the official Open Library Data Dumps page. 
After downloading and extracting the dumps, you can place the .txt files into the ./data folder to be indexed by this application.

The repository's data folder contains truncated Open Library dump files for testing purposes. 
## Features

- **Indexes Open Library data** into Elasticsearch.
- **Interactive CLI** for selecting files and indices.
- **Bulk indexing**.
- **Support for Elasticsearch index management** (delete and recreate indexes as needed).
- **Configurable properties** via `application.properties` or environment variables.
- **Deployable as a standalone JAR or via Docker Compose**.

## Prerequisites

### General
- Java 21+
- Maven
- Elasticsearch (if running locally, must be compatible with version `8.15.1`)
- .txt dump files in the data folder

### For Docker Deployment
- Docker
- Docker Compose

## Deployment

### With Docker (Recommended)

1. **Clone the repository:**
   ```sh
   git clone https://github.com/nebl-annamaria/openlibrary-elasticsearch.git
   cd openlibrary-es
   ```
2. **Build the Docker image:**
   ```sh
   docker build -t openlibrary-es .
   ```
3. **Start Elasticsearch and the application:**

This application requires an interactive terminal for user input.
When running in Docker, you must ensure the
correct flags are used to attach an interactive session.

Using docker compose, the stdin_open: true and tty: true options are already set in the docker-compose.yml file.
   ```sh
   docker compose up -d && docker attach openlibrary-app
   ```


### Without Docker (Local Environment)

1. **Clone the repository:**
   ```sh
   git clone https://github.com/nebl-annamaria/openlibrary-elasticsearch.git
   cd openlibrary-es
   ```
2. **Build the project:**
   ```sh
   mvn clean package
   ```
3. **Run Elasticsearch manually** (if not already running):
   ```sh
   docker run -d --name elasticsearch \
   -p 9200:9200 \
   -e "discovery.type=single-node" \
   -e "ELASTIC_PASSWORD=your-secure-password" \
   -e "xpack.security.enabled=false" \
   -e "xpack.security.http.ssl.enabled=false" \
   docker.elastic.co/elasticsearch/elasticsearch:8.15.1
   ```
4. **Run the application:**
   ```sh
   java -jar target/openlibrary-es.jar
   ```

## Working with the Application

1. Ensure that the ./data folder contains Open Library .txt files. (test files provided)
2. Run the application.
3. Follow the CLI prompts to:
   - Select the files to index
   - Choose the corresponding index type (Authors, Works, Editions)
   - Optionally delete existing indices before indexing
4. Once finished, you can inspect the indexed data in Kibana or via `curl`:
   ```sh
   curl -u elastic:your-secure-password http://localhost:9200/authors/_count\?pretty\=true   
   ```

## Configuration

You can customize application settings via `application.properties` or environment variables. 
You can also change these values in the `docker-compose.yml` file:

```properties
spring.elasticsearch.uris=${SPRING_ELASTICSEARCH_URIS:localhost:9200}
spring.elasticsearch.username=${SPRING_ELASTICSEARCH_USERNAME:elastic}
spring.elasticsearch.password=${SPRING_ELASTICSEARCH_PASSWORD:your-secure-password}
app.data-folder=${APP_DATA_FOLDER:./data}
```

To override these values, set them as environment variables before running the app:
```sh
export SPRING_ELASTICSEARCH_USERNAME=myuser
export SPRING_ELASTICSEARCH_PASSWORD=mypassword
java -jar target/openlibrary-es.jar
```

### ⚠️ **Security Warning: Do Not Use This Configuration in Production**

The application is configured to run Elasticsearch with SSL disabled 
(xpack.security.http.ssl.enabled=false). 
This setup simplifies local development and testing, 
but should not be used in a production environment, 
as it leaves the Elasticsearch instance vulnerable. 
In a production setup, proper security configurations, 
including authentication, SSL encryption, and role-based access control, 
should be enabled to protect sensitive data.

## License

MIT License. See `LICENSE` file for details.

---
For questions or contributions, feel free to open an issue or submit a pull request!

