# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**IntelliConnect** - The first Agent-based AI IoT platform. An AI-powered Internet of Things platform built with Spring Boot 3.5.7 and Java 21, featuring multi-agent architecture, voice capabilities (ASR/TTS/VAD), RAG knowledge bases, native knowledge graphs, MCP (Model Context Protocol) support, and complete IoT infrastructure with Thing Model abstraction.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.5.7, Spring Security, JWT, Maven
- **Frontend**: Vue 3 (in `/web` directory)
- **AI/LLM**: LangChain4j, Dashscope/Qwen3, GLM/Zhipu, DeepSeek, SiliconFlow, UniApi
- **Voice**: ASR (Dashscope/Funasr), TTS (MiniMax, Edge TTS, Xunfei), VAD (ONNX Runtime), Opus codec
- **Databases**: MySQL 8.0 (JPA/Hibernate), Redis, InfluxDB (time-series), ChromaDB (vector)
- **IoT**: EMQX 5.8.4 MQTT broker with gRPC ExHook integration
- **Other**: gRPC, WebSocket, Quartz scheduling, Thymeleaf, Jasypt encryption

## Build Commands

### Backend (Maven)

```bash
# Clean and compile
./mvnw clean compile

# Build executable JAR (runs tests by default)
./mvnw clean package

# Build executable JAR, skip tests
./mvnw clean package -DskipTests

# Run code formatting with Spotless
./mvnw spotless:apply

# Run tests
./mvnw test

# Encrypt configuration values with Jasypt
./mvnw jasypt:encrypt -Djasypt.encryptor.password=your-password

# On Windows use
mvnw.cmd clean package -DskipTests
```

### Frontend (Vue 3)

```bash
cd web

# Install dependencies
npm install

# Start development server
npm run dev

# Production build
npm run build
```

### Infrastructure (Docker)

```bash
cd docker
docker-compose up -d
```
Starts MySQL 8.0, Redis, InfluxDB, EMQX 5.8.4, ChromaDB, and optional Funasr service.

## Run the Application

```bash
# After building
java -jar target/IntelliConnect-1.8-SNAPSHOT.jar
```

The application starts on port `8080` by default. gRPC server for EMQX ExHook listens on port `9090`.

## Project Architecture

### Backend Layered Structure (src/main/java/top/rslly/iot/)

- `config/` - Spring configuration classes (CORS, Redis, security, etc.)
- `controllers/` - REST API controllers
  - `Auth.java` - Authentication endpoints
  - `Tool.java` - AI/Tool endpoints (main functionality)
  - `Wx.java` - WeChat integration
  - `XiaoZhi.java` - XiaoZhi ESP32 hardware support
- `dao/` - Data access repositories
- `error/` - Global error handling
- `models/` - JPA entity models
  - `models/influxdb/` - InfluxDB data point models
- `param/` - Request/response DTOs
- `services/` - Business logic layer
  - `services/agent/` - AI Agent services
  - `services/iot/` - IoT core services (OTA, MQTT, Alarm, Hardware management)
  - `services/thingsModel/` - Thing Model implementation (Product, Device, Event, Function)
  - `services/knowledgeGraphic/` - Knowledge graph services
  - `services/storage/` - Storage services
  - `services/wechat/` - WeChat integration services
- `transfer/mqtt/` - MQTT message handling
- `utility/` - Utilities and helpers
  - `utility/ai/` - AI utilities
    - `utility/ai/llm/` - LLM provider integrations
    - `utility/ai/mcp/` - Model Context Protocol implementation
    - `utility/ai/rag/` - RAG implementation
    - `utility/ai/toolAgent/` - Tool Agent implementation
    - `utility/ai/tools/` - AI tool definitions
    - `utility/ai/voice/` - ASR, TTS, audio processing
  - `utility/exhook/` - EMQX ExHook gRPC generated code
  - `utility/properties/` - Configuration property bindings
  - `utility/result/` - Result wrapper classes

### Key Architectural Patterns

- **MVC Layered Architecture** - Clear separation between controller, service, and data access layers
- **Thing Model Abstraction** - Standard abstraction for IoT devices with properties, functions, and events
- **Multi-Agent Architecture** - Different AI agents for different capabilities with role-based configuration
- **MCP Integration** - Extensible tool calling via Model Context Protocol
- **RAG Knowledge Bases** - Document processing and vector search with ChromaDB

## Configuration

Main configuration file: `src/main/resources/application.yaml`

Key configuration sections:
- `server` - HTTP server settings (virtual threads enabled)
- `spring.datasource` - MySQL connection with Druid connection pool
- `spring.jpa` - Hibernate configuration (`ddl-auto: update` recommended for development)
- `spring.data.redis` - Redis connection settings
- `influxdb` - InfluxDB connection for time-series data
- `mqtt` - EMQX MQTT broker configuration
- `grpc` - gRPC server settings for EMQX ExHook
- `wx` - WeChat AppID/AppSecret configuration
- `ota` - OTA firmware storage paths
- `rag` - Embedding model and ChromaDB settings
- `ai` - LLM API keys, ASR/TTS provider selection, voice settings
- `jwt` - JWT secret key for authentication
- `jasypt` - Encrypted configuration password

## Code Formatting

- Uses Spotless with Eclipse formatter configuration
- Configuration: `dev-support/dailymart_spotless_formatter.xml`
- Excludes `src/main/java/top/rslly/iot/utility/ai/voice/concentus/**` from formatting
- Run `./mvnw spotless:apply` before committing changes

## Important Notes

- Project is dual-licensed: Apache 2.0 for personal use, commercial license required for commercial use
- Maintains backward compatibility with xiaozhi-esp32 protocol
- Only 2 existing test files - write tests for new features when appropriate
- Configuration uses YAML format, not properties
- Sensitive data should be encrypted using Jasypt
- gRPC proto files are in `src/main/proto/`

## Documentation

- Official documentation: https://ruanrongman.github.io/IntelliConnect/
- GitHub repository: https://github.com/ruanrongman/IntelliConnect
- Community: https://github.com/cwliot
