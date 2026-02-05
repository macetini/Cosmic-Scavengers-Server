# Cosmic Scavengers — Java Server

A Java server backend for the Cosmic Scavengers game. This repository contains the game server responsible for session management, game state, player connections, and game logic that communicates with the Unity client.

Status
- Draft README — update with exact build commands, runtime configuration, and protocol details from the repository.
- Language: Java

Features
- Multiplayer session & game state management
- Player authentication and matchmaking (placeholder — fill with real features)
- Real-time messaging / game tick processing
- Persistence hooks for saving game state (if applicable)

Requirements
- Java 17+ (recommended). Adjust to the project's required JDK if different.
- Build tool: Maven or Gradle (update README with the actual build tool used in repo)
- Optional: Database (Postgres/MySQL/Redis) if the repo uses one — supply connection details

Quickstart (generic)
1. Clone the repo:
   git clone https://github.com/macetini/Cosmic-Scavengers-Java-Server.git
2. Build
   - If Maven:
     mvn clean package -DskipTests
   - If Gradle:
     ./gradlew build -x test
3. Run
   java -jar target/<your-server-jar>.jar
   or
   java -jar build/libs/<your-server-jar>.jar

Configuration
- Environment variables (examples — replace with actual names used by the project):
  - SERVER_PORT (default: 8080)
  - DB_URL, DB_USER, DB_PASSWORD
  - LOG_LEVEL
- Alternatively, edit application.properties / application.yml / config.json as present in the repository.
- Add any protocol-specific settings (tick rate, UDP/TCP ports, WebSocket endpoint) here once known.

Networking / Protocol
- This server is intended to communicate with the Unity client. Document the exact protocol here:
  - Transport (TCP / UDP / WebSockets)
  - Authentication flow (token-based / username/password)
  - Message formats (JSON / binary / protobuf)
  - Example connection and handshake sequence

Logging & Monitoring
- Logs are written to console and/or log files (configure via logback/log4j or other logging framework)
- Add guidance for metrics and monitoring (Prometheus, health endpoints) if available

Testing
- Unit tests: mvn test or ./gradlew test
- Integration tests: describe how to run them if present
- Advice: run server locally and connect client in the Unity editor for end-to-end testing

CI / CD
- Add Continuous Integration details (GitHub Actions / other) here when available
- Provide build badge and main pipeline steps in the README (build -> test -> publish)

Deployment
- Example: run on a Linux host or containerize the server:
  - Dockerfile: build jar, copy, and run with ENTRYPOINT
  - Kubernetes manifest: provide templates for Deployment, Service, and ConfigMap
- Provide environment-specific notes (ports to expose, resource requirements)

Contributing
- Fork the repo, create a feature branch, open a pull request
- Follow the project's coding style
- Add tests for new features or bug fixes
- Describe how to run linters or formatters if used

License
- Add the project's license (MIT / Apache-2.0 / proprietary). If unsure, add a LICENSE file in the repo.

Contact
- Maintainer: macetini
- For questions/issues, open an issue on GitHub.

Notes / To do
- Fill in build tool specifics (Maven/Gradle)
- Document protocol and server configuration used by the Unity client
- Add example env file and sample data for local development
