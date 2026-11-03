-- ==============================================================================
-- CineBook Supabase PostgreSQL Schema
-- Database: PostgreSQL 17 (Supabase)
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. APP_USER (Registered CineBook Users)
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) UNIQUE NOT NULL,
    password VARCHAR(255),
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotent column additions for existing app_user tables
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'ROLE_USER';

-- 2. MOVIE (Now Showing & Coming Soon Catalog)
CREATE TABLE IF NOT EXISTS movie (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    poster_url TEXT,
    backdrop_url TEXT,
    duration_minutes INT,
    language VARCHAR(50),
    genre VARCHAR(150),
    rating NUMERIC(3,1) DEFAULT 0.0,
    release_date DATE,
    trailer_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'NOW_SHOWING',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotent column additions for existing movie tables
ALTER TABLE movie ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE movie ADD COLUMN IF NOT EXISTS backdrop_url TEXT;
ALTER TABLE movie ADD COLUMN IF NOT EXISTS trailer_url TEXT;

-- 3. THEATER (Multiplexes & Cinema Halls)
CREATE TABLE IF NOT EXISTS theater (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. SCREEN (Auditoriums within Theaters)
CREATE TABLE IF NOT EXISTS screen (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theater_id UUID NOT NULL REFERENCES theater(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    rows_count INT NOT NULL DEFAULT 6,
    seats_per_row INT NOT NULL DEFAULT 10
);

-- 5. SHOW (Screenings per Screen, Date, and Time)
CREATE TABLE IF NOT EXISTS "show" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL REFERENCES movie(id) ON DELETE CASCADE,
    screen_id UUID NOT NULL REFERENCES screen(id) ON DELETE CASCADE,
    show_date DATE NOT NULL,
    start_time TIME NOT NULL,
    format VARCHAR(30) NOT NULL DEFAULT '2D',
    base_price NUMERIC(10,2) NOT NULL DEFAULT 250.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    booking_open BOOLEAN NOT NULL DEFAULT true
);

-- Idempotent column additions for existing show tables
ALTER TABLE "show" ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE "show" ADD COLUMN IF NOT EXISTS booking_open BOOLEAN DEFAULT true;

-- 6. BOOKING (Confirmed & Cancelled Digital Tickets)
CREATE TABLE IF NOT EXISTS booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID REFERENCES "show"(id) ON DELETE SET NULL,
    user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    booking_code VARCHAR(40) UNIQUE NOT NULL,
    user_email VARCHAR(180) NOT NULL,
    user_name VARCHAR(120),
    movie_title VARCHAR(255) NOT NULL,
    theater_name VARCHAR(255) NOT NULL,
    show_time VARCHAR(100) NOT NULL,
    seats VARCHAR(255) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    total_amount NUMERIC(10,2),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    booking_date DATE NOT NULL DEFAULT CURRENT_DATE,
    cancelled_at VARCHAR(100),
    refund_amount NUMERIC(10,2),
    refund_status VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Idempotent column additions for existing booking tables
ALTER TABLE booking ADD COLUMN IF NOT EXISTS user_email VARCHAR(180);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS user_name VARCHAR(120);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS movie_title VARCHAR(255);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS theater_name VARCHAR(255);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS show_time VARCHAR(100);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS seats VARCHAR(255);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS amount NUMERIC(10,2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS total_amount NUMERIC(10,2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS booking_date DATE DEFAULT CURRENT_DATE;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS cancelled_at VARCHAR(100);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS refund_amount NUMERIC(10,2);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS refund_status VARCHAR(50);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

-- 7. PERFORMANCE INDEXES
CREATE INDEX IF NOT EXISTS idx_booking_user_email ON booking(user_email);
CREATE INDEX IF NOT EXISTS idx_booking_code ON booking(booking_code);
CREATE INDEX IF NOT EXISTS idx_show_movie_date ON "show"(movie_id, show_date);
CREATE INDEX IF NOT EXISTS idx_movie_status ON movie(status, is_active);
CREATE INDEX IF NOT EXISTS idx_theater_city ON theater(city);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
