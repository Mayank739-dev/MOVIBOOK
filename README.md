# CineBook — Movie Ticket Booking System

Full-stack movie booking starter built with:
- React + Vite
- Java 21 + Spring Boot 3
- Supabase PostgreSQL
- JWT authentication
- Responsive cinema UI

## Run

### Backend
1. Create a Supabase project.
2. Create the database tables using `backend/supabase/schema.sql`.
3. Copy `backend/.env.example` to environment variables or configure `application.properties`.
4. Run:
   `cd backend && mvn spring-boot:run`

### Frontend
`cd frontend && npm install && npm run dev`

The frontend is configured for `http://localhost:8080`.

## Important
This starter includes working movie browsing, search, movie details, show selection, interactive seats, booking flow, authentication scaffolding, and Supabase schema. Payment is intentionally a test-mode flow; replace it with a real payment gateway before production.
