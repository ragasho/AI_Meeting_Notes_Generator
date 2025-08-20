# AI Meeting Notes Generator

This repository contains a full-stack application for generating meeting notes using AI technologies. The project utilizes both Java (with Maven) for backend services and Node.js (with Tailwind CSS) for frontend development.

## Project Structure

- **.gitattributes**: Git attributes configuration.
- **.gitignore**: Specifies intentionally untracked files to ignore.
- **.mvn/**: Maven wrapper files for consistent builds.
- **mvnw / mvnw.cmd**: Maven wrapper scripts for Unix and Windows.
- **node_modules/**: Node.js dependencies (auto-generated).
- **package.json / package-lock.json**: Node.js project configuration and lock file.
- **pom.xml**: Maven project configuration for Java backend.
- **src/**: Source code for the application.
- **tailwind.config.js**: Tailwind CSS configuration.

## Getting Started

### Backend (Java with Maven)

1. Ensure you have Java and Maven installed.
2. Build the backend:
   ```bash
   ./mvnw clean install
   ```
3. Run the backend service:
   ```bash
   ./mvnw spring-boot:run
   ```

### Frontend (Node.js with Tailwind CSS)

1. Ensure you have Node.js and npm installed.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the frontend development server:
   ```bash
   npm run dev
   ```

## Features

- AI-powered meeting notes generation.
- Modern frontend built with Tailwind CSS.
- Java backend for robust and scalable APIs.

## Contributing

Contributions are welcome! Please open issues or submit pull requests for improvements.

## License

This project is licensed under the MIT License.
