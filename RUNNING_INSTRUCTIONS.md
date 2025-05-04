# Running Instructions for the Project

## Backend (Java Maven Project)

1. Ensure you have Java JDK (version 8 or above) and Maven installed on your system.

2. Open a terminal and navigate to the backend directory:
   ```
   cd backend
   ```

3. Build the project and download dependencies:
   ```
   mvn clean install
   ```

4. Run the application:
   - Option 1: Using Maven Spring Boot plugin
     ```
     mvn spring-boot:run
     ```
   - Option 2: Run the generated jar file
     ```
     java -jar target/MutationTesting-1.0-SNAPSHOT.jar
     ```

5. The backend server will start running on the configured port (usually 8080).

## Frontend (Node.js Project)

1. Ensure you have Node.js and npm installed on your system.

2. Open a terminal and navigate to the frontend root directory (where `package.json` is located):
   ```
   cd <project-root>
   ```

3. Install the dependencies:
   ```
   npm install
   ```

4. Start the development server:
   ```
   npm start
   ```
   or if the project uses a different script:
   ```
   npm run dev
   ```

5. The frontend development server will start, usually accessible at `http://localhost:3000`.

## Notes

- Make sure the backend server is running before using the frontend if they communicate.
- You can generate the `node_modules` and backend `target` folders by running the above commands; these folders are excluded from the archive.
- Adjust commands if your environment or project setup differs.

If you need any further assistance, feel free to ask.
