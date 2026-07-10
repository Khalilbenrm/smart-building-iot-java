# Smart Building IoT — Real-Time Backend

A Spring Boot backend that ingests, processes and stores IoT telemetry (temperature/humidity)
from sensors deployed across a smart building (offices, meeting rooms, corridors, server rooms,
critical HVAC zones).

## Architecture

```
REST API (POST /api/sensors/data)
        |
        v
  SensorDataService  --publish-->  Kafka topic (iot-sensor-data)
                                            |
                                            v
                                   Kafka Consumer --check anomalies--> logs
                                            |
                                            v
                                        MongoDB
```

Readings come in over a simple REST call. The service validates them, timestamps them, and
publishes them to a Kafka topic instead of writing to the database directly — this decouples
ingestion from processing and is a common pattern for high-throughput event streams. A consumer
reads from that topic, flags obvious anomalies (temperature > 30°C, humidity > 80%) with a
warning log, and persists the reading to MongoDB.

- **Controller** — pure HTTP mapping, no business logic.
- **Service** — validates business rules, timestamps readings, delegates to the Kafka producer;
  maps entities back to DTOs for reads.
- **Kafka Producer** — publishes `SensorDataDto` as JSON to `iot-sensor-data`, keyed by `deviceId`
  so all readings for one device stay ordered on the same partition.
- **Kafka Consumer** — deserializes, runs simple anomaly detection, and persists to MongoDB.
  Invalid records are logged (ERROR) and dropped rather than blocking the consumer. Processing
  failures are retried 3× (1s backoff) before being logged and skipped.
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

## Run it

You need Docker installed. That's it.

```bash
docker-compose up --build
```

This starts everything: Zookeeper, Kafka, MongoDB, and the app (port 8080). Wait until you see
the app log `Started SmartIotApplication`, then try it:

```bash
curl -X POST http://localhost:8080/api/sensors/data \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "office-01", "temperature": 22.5, "humidity": 45.0}'

curl http://localhost:8080/api/sensors/device/office-01
```

To stop everything: `docker-compose down` (add `-v` to also wipe the MongoDB volume).

### Running locally without Docker for the app

Requires Kafka and MongoDB reachable on `localhost` (you can still run just those two via
`docker-compose up zookeeper kafka mongo`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### IoT simulator (optional)

Continuously generates randomized readings for 5 zones (with occasional simulated overheating in
the server room) and feeds them straight into the service, exactly like the REST endpoint would —
useful to see data flowing without manually calling the API:

```bash
java -jar target/smart-iot-building.jar --spring.profiles.active=simulator
```

## Tests

```bash
mvn test
```

Covers the service layer (Mockito), Kafka producer/consumer (mocked `KafkaTemplate` /
`SensorDataRepository`), and the REST controller (`@WebMvcTest` + MockMvc), including validation
and anomaly paths.

## Possible improvements

- **Scalability**: partition `iot-sensor-data` by zone/building for higher parallelism; scale
  consumer group instances horizontally; move to a time-series-optimized store (e.g. MongoDB
  time-series collections or TimescaleDB) for high-cardinality telemetry.
- **Monitoring**: expose Micrometer metrics (consumer lag, ingestion rate, anomaly counts) via
  Actuator/Prometheus + Grafana dashboards; add distributed tracing (OpenTelemetry) across the
  REST → Kafka → Mongo flow.
- **CI/CD**: GitHub Actions pipeline running `mvn verify`, building/pushing the Docker image, and
  deploying via a Kubernetes Helm chart with readiness/liveness probes on `/actuator/health`.
- **Alerting**: replace log-based anomaly detection with a dedicated `iot-sensor-alerts` topic and
  a notification service (email/Slack/webhook) subscribed to it.
