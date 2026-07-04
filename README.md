# Smart Building IoT — Real-Time Event-Driven Backend

A production-grade Spring Boot backend that ingests, processes and stores IoT telemetry
(temperature/humidity) from sensors deployed across a smart building (offices, meeting rooms,
corridors, server rooms, critical HVAC zones).

## Architecture

```
REST API → Service → Kafka Producer → Kafka Topic (iot-sensor-data) → Kafka Consumer → MongoDB
```

- **Controller** — pure HTTP mapping, no business logic.
- **Service** — validates business rules, timestamps readings, delegates to the Kafka producer;
  maps entities back to DTOs for reads.
- **Kafka Producer** — publishes `SensorDataDto` as JSON to `iot-sensor-data`, keyed by `deviceId`
  so all readings for one device stay ordered on the same partition.
- **Kafka Consumer** — deserializes, runs simple anomaly detection (`temperature > 30`,
  `humidity > 80` → WARN logs), and persists to MongoDB. Invalid records are logged (ERROR) and
  dropped rather than blocking the consumer. Processing failures are retried 3× (1s backoff) via
  `DefaultErrorHandler` before being logged and skipped.
- **Repository** — MongoDB access only (`sensor_data` collection, indexed on `deviceId` and
  `timestamp`).
- **GlobalExceptionHandler** — `@RestControllerAdvice` returning `{ timestamp, error, message, path }`.

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/sensors/data` | Ingest a reading → published to Kafka, returns 202 |
| GET | `/api/sensors/device/{deviceId}` | Historical readings for a device |
| GET | `/api/sensors/range?start=&end=` | Readings within an ISO-8601 instant range |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running

```bash
docker-compose up --build
```

Spins up Zookeeper, Kafka, MongoDB and the app (port 8080).

Local dev without Docker (requires Kafka/Mongo reachable on localhost):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

###  IoT simulator

Continuously posts randomized readings for 5 zones (with occasional simulated overheating in the
server room) to the ingestion endpoint:

```bash
java -jar target/smart-iot-building.jar --spring.profiles.active=simulator
```

## Tests

```bash
mvn test
```

Covers the service layer (Mockito), Kafka producer/consumer (mocked `KafkaTemplate` /
`SensorDataRepository`), and REST controller (`@WebMvcTest` + MockMvc), including validation and
anomaly paths.

## Possible improvements

- **Scalability**: partition `iot-sensor-data` by zone/building for higher parallelism; scale
  consumer group instances horizontally; move to a time-series-optimized store (e.g. MongoDB
  time-series collections or TimescaleDB) for high-cardinality telemetry.
- **Monitoring**: expose Micrometer metrics (consumer lag, ingestion rate, anomaly counts) via
  Actuator/Prometheus + Grafana dashboards; add distributed tracing (OpenTelemetry) across the
  REST → Kafka → Mongo flow.
- **CI/CD**: GitHub Actions pipeline running `mvn verify`, building/pushing the Docker image, and
  deploying via a Kubernetes Helm chart with readiness/liveness probes on `/actuator/health`.
- **Microservices decomposition**: split into `ingestion-service` (REST + producer),
  `processing-service` (consumer + anomaly detection), and `analytics-service` (queries/reporting),
  communicating exclusively through Kafka topics.
- **Alerting**: replace log-based anomaly detection with a dedicated `iot-sensor-alerts` topic and
  a notification service (email/Slack/webhook) subscribed to it.
