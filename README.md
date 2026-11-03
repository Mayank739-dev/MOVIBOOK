# CineBook 🎬 — Movie Discovery & Ticket Booking Platform

CineBook is a full-stack movie discovery and ticket booking platform designed for real-world cinema multiplexes. Unlike generic movie databases, CineBook operates with verified, city-based theater screenings, interactive seat selection, secure authentication, and individual digital ticket management with instant cancellations.

---

## 🚀 Tech Stack

### Frontend
- **Framework**: React 18
- **Build Tool & Dev Server**: Vite
- **Routing**: React Router DOM (v6)
- **HTTP Client**: Axios
- **Iconography**: Lucide React
- **Styling**: Modern CSS3 (Custom Design Tokens, Flexbox/Grid, Dark & Light Mode themes)

### Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.5
- **ORM & Data Access**: Spring Data JPA / Hibernate ORM 6
- **Security**: Spring Security 6 with BCrypt password hashing & stateless CORS
- **JSON Serialization**: Jackson with `@JsonIgnore` / `@JsonProperty` mapping
- **Build Tool**: Apache Maven

### Database & Cloud Infrastructure
- **Database**: PostgreSQL 17 hosted on **Supabase**
- **Connection Pooling**: HikariCP (connected via Supabase IPv4 Pooler)
- **Schema Management**: Dynamic JPA schema updates (`ddl-auto=update`) + Supabase SQL scripts (`backend/supabase/schema.sql`)

### External Movie API
- **API Provider**: The Movie Database (TMDB API)
- **Capabilities**: Backdrops, posters, plot synopses, cast & crew, genres, release dates, and official YouTube trailers

---

## ✨ Key Features

1. **City-Based "Now Showing" Verification**:
   - Strictly shows movies that have active, non-expired scheduled shows in the selected city (e.g., Chandigarh, Delhi, Mumbai).
   - If an older film is re-screened (e.g., an IMAX special screening), it appears only when verified shows are scheduled.

2. **Future-Only "Coming Soon"**:
   - Strictly filters movies with release dates in the future (`release_date > CURRENT_DATE`).

3. **Free Exploration & Protected Booking**:
   - Visitors can freely browse movies, search, watch trailers in a pop-up modal, inspect cast members, and view showtimes without logging in.
   - Booking tickets or selecting seats requires signing in or creating a personal account.

4. **Authentication & Multi-User Isolation**:
   - Individual user registration and sign-in backed by encrypted passwords in Supabase.
   - Each user has their own private booking history under **My Bookings**.

5. **Interactive Seat Selection & Booking**:
   - Tiered seating layouts (Recliner, Platinum, Gold) with pricing calculations.
   - Generates unique reference booking codes (`CBK-XXXXXX`).

6. **Self-Service Ticket Cancellation**:
   - Users can cancel their booked tickets with 1-click confirmation.
   - Releases seats and initiates a 100% refund status tracking.

7. **Dark / Light Mode**:
   - One-click theme toggle in the navigation bar with full theme persistence.

---

## 🗄️ Database Architecture (Supabase)

```
[app_user] ──< [booking] >── [show] ──> [movie]
                   │            │
                   │            └──> [screen] ──> [theater]
                   │
                   └── (seats, amount, status, refund)
```

- **`app_user`**: User accounts (name, email, BCrypt password hash, role).
- **`movie`**: Theatrical catalog (title, poster, backdrop, synopsis, runtime, rating, status).
- **`theater`**: Multiplexes filtered by city.
- **`screen`**: Auditoriums per theater (Dolby Atmos, IMAX 70mm, Laser 4K).
- **`show`**: Scheduled screening dates, start times, formats, and base pricing.
- **`booking`**: Digital ticket records linked to user email, show, seat labels, status (`CONFIRMED` / `CANCELLED`), and refund tracking.

---

## 📡 Core API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Authenticate user credentials |
| `GET` | `/api/movies/now-showing?city={city}` | Movies with active shows in selected city |
| `GET` | `/api/movies/coming-soon` | Upcoming theatrical releases |
| `GET` | `/api/movies/cities` | Distinct cities with operating theaters |
| `GET` | `/api/movies/{id}` | Full movie details with cast & media |
| `GET` | `/api/movies/search?query={q}` | Debounced movie search |
| `GET` | `/api/shows/theaters?city={city}` | Theaters operating in a city |
| `GET` | `/api/shows/movie/{id}?city={city}` | Showtimes grouped by theater for a movie |
| `GET` | `/api/shows/{id}/seats` | Seat availability map for a screening |
| `GET` | `/api/bookings?email={email}` | User's active & cancelled ticket history |
| `POST` | `/api/bookings` | Create and confirm a new booking |
| `POST` | `/api/bookings/{code}/cancel` | Cancel booking and trigger 100% refund |

---

## 🛠️ Running Locally

### 1. Backend (Spring Boot)
Ensure Java 21 and Maven are installed:
```bash
cd backend
mvn spring-boot:run
```
Backend runs on: `http://localhost:8080`

### 2. Frontend (React + Vite)
Ensure Node.js is installed:
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on: `http://localhost:5174` (or configured Vite port)

---

## 📄 License
MIT License. Created for moviegoers and cinema ticket reservation platforms.
