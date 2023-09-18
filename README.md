# Cloud file storage
Multi-user file cloud. Users of the service can use it to upload and store files.

## Functionality
### Users:
- sign up
- log in
- log out

### Files and folders:
- uploading
- renaming
- moving
- downloading
- deleting

## Technology stack
- Spring Boot
- Spring Security
- Spring MVC + Thymeleaf + Bootstrap
- Spring Sessions + Redis
- Data persistence:
  - Spring Data JPA + PostgreSQL for User info
  - MinIO for files
- Junit5 + Testcontainers + Hamcrest for testing
- Docker compose for deployment

## Local deployment guide
1. Clone repository

```shell
git clone https://github.com/TurboGoose/cloud-file-storage.git
```

2. `cd` to the root folder of the cloned repository 
3. Run Docker compose stack for local development (Docker have to be installed and running)

```shell
docker compose -f compose/docker-compose.yml up
```

3. Run application

```shell
./mvnw spring-boot:run
```