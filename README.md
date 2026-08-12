# Full-Stack Application

A full-stack web application built with **Spring Boot** and **Vite**.

## Tech Stack

* Java
* Spring Boot
* Spring Security
* Maven
* Vite
* JavaScript / TypeScript
* npm

## Prerequisites

Make sure the following are installed:

* Java
* Node.js
* npm
* Git

Check versions:
java -version
node -v
npm -v
git --version

## Backend Setup
Navigate to the backend:
cd backend/user-service
mvnw clean install
Set the required environment variables in application.yaml:

export CLIENT_ID="YOUR_CLIENT_ID"
export CLIENT_SECRET="YOUR_CLIENT_SECRET"
Run the Spring Boot application:

./mvnw spring-boot:run
Backend:
http://localhost:8084

## Frontend Setup
Navigate to the frontend:
cd frontend
Install dependencies:
npm install
Create `frontend/.env`:
VITE_API_BASE_URL=http://localhost:8080
Start the development server:
npm run dev
Frontend:
http://localhost:5173

## Environment Variables

### Backend
CLIENT_ID=YOUR_CLIENT_ID
CLIENT_SECRET=YOUR_CLIENT_SECRET

### Frontend
VITE_API_BASE_URL=http://localhost:8080

## Build

Backend:
cd backend
./mvnw clean package

Frontend:
cd frontend
npm run build
