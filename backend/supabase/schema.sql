create extension if not exists pgcrypto;

create table if not exists app_user (
 id uuid primary key default gen_random_uuid(),
 name varchar(120) not null,
 email varchar(180) unique not null,
 password_hash varchar(255) not null,
 role varchar(20) not null default 'USER',
 created_at timestamptz not null default now()
);

create table if not exists movie (
 id uuid primary key default gen_random_uuid(),
 title varchar(180) not null,
 description text,
 poster_url text,
 backdrop_url text,
 duration_minutes int not null,
 language varchar(50) not null,
 genre varchar(120),
 rating numeric(3,1) default 0,
 release_date date,
 trailer_url text,
 status varchar(20) not null default 'NOW_SHOWING',
 created_at timestamptz not null default now()
);

create table if not exists theater (
 id uuid primary key default gen_random_uuid(),
 name varchar(180) not null,
 city varchar(100) not null,
 address text,
 created_at timestamptz not null default now()
);

create table if not exists screen (
 id uuid primary key default gen_random_uuid(),
 theater_id uuid not null references theater(id) on delete cascade,
 name varchar(100) not null,
 rows_count int not null,
 seats_per_row int not null
);

create table if not exists seat (
 id uuid primary key default gen_random_uuid(),
 screen_id uuid not null references screen(id) on delete cascade,
 row_label varchar(5) not null,
 seat_number int not null,
 seat_type varchar(30) not null default 'GOLD',
 price numeric(10,2) not null,
 unique(screen_id,row_label,seat_number)
);

create table if not exists show (
 id uuid primary key default gen_random_uuid(),
 movie_id uuid not null references movie(id) on delete cascade,
 screen_id uuid not null references screen(id) on delete cascade,
 show_date date not null,
 start_time time not null,
 format varchar(20) not null default '2D',
 base_price numeric(10,2) not null
);

create table if not exists show_seat (
 id uuid primary key default gen_random_uuid(),
 show_id uuid not null references show(id) on delete cascade,
 seat_id uuid not null references seat(id) on delete cascade,
 status varchar(20) not null default 'AVAILABLE',
 locked_until timestamptz,
 booking_id uuid,
 unique(show_id,seat_id)
);

create table if not exists booking (
 id uuid primary key default gen_random_uuid(),
 user_id uuid references app_user(id),
 show_id uuid not null references show(id),
 booking_code varchar(30) unique not null,
 status varchar(20) not null default 'CONFIRMED',
 total_amount numeric(10,2) not null,
 created_at timestamptz not null default now()
);

alter table show_seat drop constraint if exists show_seat_booking_fk;
alter table show_seat add constraint show_seat_booking_fk foreign key (booking_id) references booking(id);

create table if not exists booking_seat (
 booking_id uuid references booking(id) on delete cascade,
 show_seat_id uuid references show_seat(id),
 primary key(booking_id,show_seat_id)
);

create index if not exists idx_movie_title on movie(title);
create index if not exists idx_show_date on show(show_date);
create index if not exists idx_show_seat_show on show_seat(show_id);

-- Seed demo data
insert into movie(title,description,poster_url,backdrop_url,duration_minutes,language,genre,rating,release_date,status)
select 'Midnight Protocol','A cyber-thriller about an elite team racing against a city-wide blackout.','https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1600&q=80',128,'English','Action • Thriller',8.7,current_date,'NOW_SHOWING'
where not exists (select 1 from movie where title='Midnight Protocol');

insert into movie(title,description,poster_url,backdrop_url,duration_minutes,language,genre,rating,release_date,status)
select 'Neon Dreams','A romantic sci-fi journey through a future city where memories can be replayed.','https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1600&q=80',116,'Hindi','Sci-Fi • Romance',8.2,current_date,'NOW_SHOWING'
where not exists (select 1 from movie where title='Neon Dreams');

insert into movie(title,description,poster_url,backdrop_url,duration_minutes,language,genre,rating,release_date,status)
select 'The Last Kingdom','A warrior returns home to defend his people from an invading army.','https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?auto=format&fit=crop&w=600&q=80','https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?auto=format&fit=crop&w=1600&q=80',142,'English','Drama • Action',9.0,current_date,'COMING_SOON'
where not exists (select 1 from movie where title='The Last Kingdom');
