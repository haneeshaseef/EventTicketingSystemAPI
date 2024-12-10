# Event Ticketing System API

## Project Overview
This is a Spring Boot backend application for an event ticketing system, designed to manage real time ticket purchasing and event management. The application provides RESTful APIs for user registration, event configuration management, concurrent ticket purchase,customer managemnet and vendor management. The ticketing system is implemented using a multi-threaded approach to handle concurrent ticket management operations for customes and vendors

## Technologies Used
- Java 17
- Spring Boot 3.x
- Spring MongoDB
- Maven
- Swagger/OpenAPI for documentation

## Prerequisites
- JDK 17
- Maven 3.8+

## Setup and Installation

### 1. Clone the Repository
```bash
git clone https://github.com/haneeshaseef/EventTicketingSystemAPI.git
cd event-ticketing-system-api
```

### 2. Database Configuration
- Create a MongoDB database collection named `EventTicketingSystem`
- Update `src/main/resources/application.properties` with your database credentials:
```properties
spring.data.mongodb.host=host_name
spring.data.mongodb.port=port_number
spring.data.mongodb.database=database_name
```

### 3. Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

## Project Structure
```
src/
├── main/
│   ├── org/coursework/eventticketingsystemapi/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST API controllers
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Database repositories
│   │   ├── model/          # Entity classes
│   │   └── exception       # Custom exceptions
│   └── resources/
│       ├── application.properties
└── test/                   # Unit and integration tests
```

## Key Features
- User registration and authentication
- Event configuration management
- Ticket management
- Vendor management
- Customer management
- admin control
- Multi Threatening for customers and vendors

## API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- API web page: `https://haneeshaseef.github.io/EventTicketingSystemAPI/`

## Troubleshooting
- Ensure MongoDB is running
- Check database connection parameters
- Verify Java and Maven versions
- Review application logs for specific errors
