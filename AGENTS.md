# Repository Guidelines

## Project Structure & Module Organization
IntelliConnect is a Spring Boot 3.5 / Java 21 backend with a Vue 3 + Vite frontend. Backend code lives in `src/main/java/top/rslly/iot/` (`controllers/`, `services/`, `dao/`, `models/`, `param/`, `config/`, `utility/`). Resources are in `src/main/resources/`, protobuf files in `src/main/proto/`, and tests in `src/test/java/`. Frontend code is under `web/src/`; docs are in `docs/`; infrastructure is in `docker/`. Avoid `target/` and generated cache/temp directories.

## Compiler & Environment
Use JDK 21 for backend compilation. Local Windows path: `C:\Users\Lenovo\.jdks\ms-21.0.9`. If the compiler is missing, set IntelliJ Project SDK, language level, Maven Runner JRE, and `JAVA_HOME` to this JDK. PowerShell: `$env:JAVA_HOME='C:\Users\Lenovo\.jdks\ms-21.0.9'; $env:Path="$env:JAVA_HOME\bin;$env:Path"`. Avoid JDK 17 or JDK 25.

## Build, Test, and Development Commands
- `mvnw.cmd clean compile` or `./mvnw clean compile`: compile the backend.
- `mvnw.cmd test` or `./mvnw test`: run backend JUnit tests.
- `mvnw.cmd clean package -DskipTests`: build the runnable JAR without tests.
- `mvnw.cmd spotless:apply`: apply Java formatting from `dev-support/dailymart_spotless_formatter.xml`.
- `cd web && npm install`: install frontend dependencies.
- `cd web && npm run dev`: start the Vite dev server.
- `cd web && npm run build`: create a production frontend build.
- `cd docker && docker-compose up -d`: start MySQL, Redis, InfluxDB, EMQX, and ChromaDB.

## Coding Style & Naming Conventions
Use 4-space indentation for Java. Classes use `PascalCase`, methods and fields use `camelCase`, and constants use `UPPER_SNAKE_CASE`. Keep backend changes layered: controllers delegate to services, services to repositories. Run Spotless before committing Java changes. Frontend code follows Vue 3 SFC patterns, ESLint, Prettier, and Stylelint.

## Testing Guidelines
Backend tests use Spring Boot Test/JUnit 5; name tests `*Test.java` and place them under matching packages in `src/test/java/`. Add focused tests for service logic, controllers, security behavior, and protocol parsing when touched. No frontend test runner is defined; validate frontend changes with `npm run build` and manual UI checks.

## Commit & Pull Request Guidelines
Commits follow Conventional Commits enforced by `web/commitlint.config.js`: `feat: add agent text-debug`, `fix(auth): refresh expired token`, `docs(api): update device docs`. Keep messages in English and use scopes when helpful. PRs should include a description, linked issues, test/build results, and screenshots for UI changes.

## Security & Configuration Tips
Keep secrets out of git. Configure local values in `src/main/resources/application.yaml` or environment-specific overrides, and use Jasypt for encrypted values when needed. Review changes touching JWT, Spring Security, MQTT credentials, AI provider keys, or database settings carefully.
