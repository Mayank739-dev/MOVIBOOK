import React, { useEffect, useState, useRef } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, useNavigate, useParams, useSearchParams, Link, useLocation } from 'react-router-dom';
import axios from 'axios';
import {
  Search, MapPin, ChevronRight, ChevronLeft, Clock, Star, ArrowLeft,
  Check, Ticket, User, Play, X, Calendar, DollarSign, Film, ExternalLink,
  Flame, TrendingUp, Award, Sparkles, Filter, AlertCircle, RefreshCw, Globe, Tag,
  XCircle, AlertTriangle, CheckCircle2, Sun, Moon
} from 'lucide-react';
import './styles.css';

const API = 'http://localhost:8080/api';

// ==========================================
// UTILITY FUNCTIONS
// ==========================================

function formatCurrency(amount) {
  if (!amount || amount <= 0) return 'Undisclosed';
  if (amount >= 1_000_000_000) {
    return '$' + (amount / 1_000_000_000).toFixed(1).replace(/\.0$/, '') + 'B';
  }
  if (amount >= 1_000_000) {
    return '$' + (amount / 1_000_000).toFixed(1).replace(/\.0$/, '') + 'M';
  }
  if (amount >= 1_000) {
    return '$' + (amount / 1_000).toFixed(1).replace(/\.0$/, '') + 'K';
  }
  return '$' + Number(amount).toLocaleString();
}

function formatRuntime(minutes) {
  if (!minutes || minutes <= 0) return 'N/A';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function formatDate(dateStr) {
  if (!dateStr) return 'TBA';
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return dateStr;
  }
}

function extractYoutubeKey(video) {
  if (!video) return null;
  if (typeof video === 'string') {
    if (video.includes('v=')) return video.split('v=')[1]?.split('&')[0];
    if (video.includes('youtu.be/')) return video.split('youtu.be/')[1]?.split('?')[0];
    return video;
  }
  return video.key || (video.videoUrl?.includes('v=') ? video.videoUrl.split('v=')[1] : null);
}

// Global active city state helper
function getStoredCity() {
  return localStorage.getItem('cinebook_city') || 'Chandigarh';
}

function setStoredCity(city) {
  localStorage.setItem('cinebook_city', city);
  window.dispatchEvent(new Event('city_changed'));
}

// Global Theme state helper (Dark / Light)
function getStoredTheme() {
  return localStorage.getItem('cinebook_theme') || 'dark';
}

function setStoredTheme(theme) {
  localStorage.setItem('cinebook_theme', theme);
  document.documentElement.setAttribute('data-theme', theme);
  window.dispatchEvent(new Event('theme_changed'));
}

// Initialize theme immediately on script evaluation
if (typeof document !== 'undefined') {
  document.documentElement.setAttribute('data-theme', getStoredTheme());
}

// ==========================================
// USER AUTHENTICATION STATE & GUARDS
// ==========================================

function getStoredUser() {
  try {
    const raw = localStorage.getItem('cinebook_current_user');
    if (!raw || raw === 'null' || raw === 'undefined' || raw === '') {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (parsed && parsed.email) {
      return parsed;
    }
    return null;
  } catch (e) {
    return null;
  }
}

function setStoredUser(user) {
  if (user && user.email) {
    const initials = user.initials || (user.name ? user.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() : 'U');
    const enriched = { ...user, initials, avatarBg: user.avatarBg || '#ff474c' };
    localStorage.setItem('cinebook_current_user', JSON.stringify(enriched));
  } else {
    localStorage.setItem('cinebook_current_user', 'null');
  }
  window.dispatchEvent(new Event('user_changed'));
}

function logoutUser() {
  localStorage.setItem('cinebook_current_user', 'null');
  window.dispatchEvent(new Event('user_changed'));
}

// Global booking guard helper:
// Allows free exploration, but intercepts ticket booking if unauthenticated
function requireAuth(message, onSuccess) {
  const user = getStoredUser();
  if (user && user.email) {
    if (onSuccess) onSuccess(user);
    return true;
  }
  window.dispatchEvent(new CustomEvent('open_auth_modal', {
    detail: { message, onSuccess }
  }));
  return false;
}

// ==========================================
// REUSABLE COMPONENTS
// ==========================================

function ImageWithFallback({ src, alt, className, fallbackType = 'poster' }) {
  const [error, setError] = useState(!src);
  const fallbackImg = fallbackType === 'backdrop'
    ? 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1600&q=80'
    : fallbackType === 'profile'
    ? 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80'
    : 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80';

  let finalSrc = src;
  // Fallback for Inception if horizontal backdrop path is passed
  if (finalSrc && finalSrc.includes('edv5CZvWj09upOsy2Y6IwDhK8bt')) {
    finalSrc = 'https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg';
  }

  return (
    <img
      src={error || !finalSrc ? fallbackImg : finalSrc}
      alt={alt || 'Movie'}
      className={className}
      loading="lazy"
      onError={() => setError(true)}
    />
  );
}

// Reusable MovieCard component with strict vertical card dimensions
function MovieCard({ m, badgeText }) {
  const nav = useNavigate();
  if (!m) return null;
  const year = m.releaseDate ? m.releaseDate.split('-')[0] : '';
  const isComingSoon = m.status === 'COMING_SOON';
  const isImax = m.title?.toUpperCase().includes('IMAX') || m.format?.toUpperCase().includes('IMAX');

  const handleBookClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (isComingSoon) {
      nav(`/movie/${m.id}`);
    } else {
      requireAuth(`Sign in required to book tickets for ${m.title}`, () => {
        nav(`/show/${m.id}`);
      });
    }
  };

  return (
    <Link to={`/movie/${m.id}`} className="carouselCard" title={`Explore ${m.title}`}>
      <div className="poster">
        <ImageWithFallback src={m.posterUrl} alt={m.title} />

        {/* Floating Badges */}
        {badgeText ? (
          <span className="badgeRank">
            <Sparkles size={11} color="#f3c969" /> {badgeText}
          </span>
        ) : isComingSoon ? (
          <span className="badgeRank soon">
            COMING SOON
          </span>
        ) : null}

        {isImax && <span className="formatPill">IMAX</span>}

        {m.rating ? (
          <span className="rating">
            <Star size={12} fill="currentColor" /> {Number(m.rating).toFixed(1)}
          </span>
        ) : null}

        {/* Hover Action: Explore or Book */}
        <div className="posterHoverOverlay">
          <button type="button" className="btnQuickBook" onClick={handleBookClick}>
            <Ticket size={14} /> {isComingSoon ? 'View Details' : 'Book Tickets'}
          </button>
        </div>
      </div>

      <div className="ct">
        <h3 title={m.title}>{m.title}</h3>
        <div className="genreBadges">
          {m.genres && m.genres.length > 0 ? (
            m.genres.slice(0, 2).map((g) => (
              <span key={g} className="genreBadge">{g}</span>
            ))
          ) : (
            <span className="genreBadge">Cinema</span>
          )}
          {m.language && <span className="genreBadge neutral">{m.language}</span>}
        </div>
        <div className="metaFooter">
          <span>{isComingSoon ? `Releasing ${formatDate(m.releaseDate)}` : (year || '2026')}</span>
          {m.durationMinutes ? <span>• {formatRuntime(m.durationMinutes)}</span> : null}
        </div>
      </div>
    </Link>
  );
}

// Horizontal Carousel with prev/next buttons
function MovieCarousel({ title, subtitle, items = [], linkTo, linkText = 'View all', emptyMessage }) {
  const trackRef = useRef(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(true);

  const checkScroll = () => {
    if (!trackRef.current) return;
    const { scrollLeft, scrollWidth, clientWidth } = trackRef.current;
    setCanScrollLeft(scrollLeft > 10);
    setCanScrollRight(scrollLeft + clientWidth < scrollWidth - 10);
  };

  useEffect(() => {
    checkScroll();
  }, [items]);

  const scroll = (direction) => {
    if (!trackRef.current) return;
    const offset = direction === 'left' ? -460 : 460;
    trackRef.current.scrollBy({ left: offset, behavior: 'smooth' });
    setTimeout(checkScroll, 300);
  };

  return (
    <section className="carouselSection">
      <div className="carouselHeader">
        <div className="carouselTitleGroup">
          {subtitle && <span className="eyebrow">{subtitle}</span>}
          <h2>{title}</h2>
        </div>
        {items && items.length > 0 && (
          <div className="carouselActions">
            {linkTo && (
              <Link to={linkTo} className="navlink" style={{ color: 'var(--accent)', fontWeight: 600 }}>
                {linkText} <ChevronRight size={16} />
              </Link>
            )}
            <div className="carouselNavBtns">
              <button
                className="carouselNavBtn"
                disabled={!canScrollLeft}
                onClick={() => scroll('left')}
                title="Previous"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                className="carouselNavBtn"
                disabled={!canScrollRight}
                onClick={() => scroll('right')}
                title="Next"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        )}
      </div>

      {items && items.length > 0 ? (
        <div className="carouselTrack" ref={trackRef} onScroll={checkScroll}>
          {items.map((m, idx) => (
            <MovieCard m={m} key={m.id || idx} />
          ))}
        </div>
      ) : (
        <div style={{ padding: '0 6vw' }}>
          <div className="searchStateBox" style={{ padding: '35px 20px' }}>
            <Film size={34} color="#777" style={{ margin: '0 auto 10px' }} />
            <p style={{ margin: 0 }}>{emptyMessage || 'No movies currently available in this section.'}</p>
          </div>
        </div>
      )}
    </section>
  );
}

// Trailer Modal
function TrailerModal({ videoKey, onClose }) {
  if (!videoKey) return null;
  return (
    <div className="modalOverlay" onClick={onClose}>
      <div className="modalContent" onClick={(e) => e.stopPropagation()}>
        <div className="modalHeader">
          <h3>Movie Trailer</h3>
          <button className="modalCloseBtn" onClick={onClose}>
            <X size={20} />
          </button>
        </div>
        <div className="videoIframeWrapper">
          <iframe
            src={`https://www.youtube-nocookie.com/embed/${videoKey}?autoplay=1&rel=0`}
            title="Movie Trailer"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>
      </div>
    </div>
  );
}

// Authentication Modal (Sign In / Register)
function AuthModal({ isOpen, onClose, noticeMessage, onSuccess }) {
  const [tab, setTab] = useState('login'); // 'login' | 'register'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setError('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please provide both email and password.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await axios.post(`${API}/auth/login`, { email, password });
      if (res.data?.success && res.data?.user) {
        setStoredUser(res.data.user);
        if (onSuccess) onSuccess(res.data.user);
        onClose();
      } else {
        setError(res.data?.message || 'Invalid credentials');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your email and password.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    if (!name || !email || !password) {
      setError('All fields are required.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const res = await axios.post(`${API}/auth/register`, { name, email, password });
      if (res.data?.success && res.data?.user) {
        setStoredUser(res.data.user);
        if (onSuccess) onSuccess(res.data.user);
        onClose();
      } else {
        setError(res.data?.message || 'Registration failed');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Email may already be in use.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="authModalOverlay" onClick={onClose}>
      <div className="authModalCard" onClick={(e) => e.stopPropagation()}>
        <div className="authModalHeader">
          <div className="authModalTitleGroup">
            <span className="eyebrow">CINEBOOK ACCOUNT</span>
            <h3>{tab === 'login' ? 'Sign In to CineBook' : 'Create New Account'}</h3>
          </div>
          <button className="authModalClose" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>

        {noticeMessage && (
          <div className="authRequirementNotice">
            <Ticket size={16} />
            <span>{noticeMessage}</span>
          </div>
        )}

        <div className="authTabs">
          <button
            className={`authTab ${tab === 'login' ? 'active' : ''}`}
            onClick={() => { setTab('login'); setError(''); }}
          >
            🔑 Sign In
          </button>
          <button
            className={`authTab ${tab === 'register' ? 'active' : ''}`}
            onClick={() => { setTab('register'); setError(''); }}
          >
            ✨ Create Account
          </button>
        </div>

        {error && (
          <div className="authErrorBox">
            <AlertCircle size={15} /> <span>{error}</span>
          </div>
        )}

        {tab === 'login' && (
          <form className="authForm" onSubmit={handleLogin}>
            <div className="authField">
              <label>Email Address</label>
              <input
                type="email"
                placeholder="e.g. user@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="authField">
              <label>Password</label>
              <input
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="primary big" style={{ width: '100%', marginTop: 8 }} disabled={loading}>
              {loading ? 'Signing In...' : 'Sign In'}
            </button>
            <p className="authFooterHint">
              Don't have an account yet?{' '}
              <button
                type="button"
                className="authInlineSwitch"
                onClick={() => { setTab('register'); setError(''); }}
              >
                Create an account
              </button>
            </p>
          </form>
        )}

        {tab === 'register' && (
          <form className="authForm" onSubmit={handleRegister}>
            <div className="authField">
              <label>Full Name</label>
              <input
                type="text"
                placeholder="e.g. Mayank Kumar"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="authField">
              <label>Email Address</label>
              <input
                type="email"
                placeholder="e.g. user@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="authField">
              <label>Password</label>
              <input
                type="password"
                placeholder="Create password (min 6 characters)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
            <button type="submit" className="primary big" style={{ width: '100%', marginTop: 8 }} disabled={loading}>
              {loading ? 'Creating Account...' : 'Create Account'}
            </button>
            <p className="authFooterHint">
              Already have an account?{' '}
              <button
                type="button"
                className="authInlineSwitch"
                onClick={() => { setTab('login'); setError(''); }}
              >
                Sign in here
              </button>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}

// Global Site Footer
function Footer() {
  return (
    <footer className="siteFooter">
      <div className="footerContent">
        <div className="footerBrand">
          <h3><span>🎬</span> CineBook</h3>
          <p>
            Your premier destination for verified cinema screenings, instant IMAX seat reservations, and exclusive promotions across top multiplexes.
          </p>
        </div>
        <div className="footerLinksCol">
          <h4>Cities</h4>
          <ul>
            <li><span style={{ color: 'var(--muted)' }}>Chandigarh (Elante & Wave)</span></li>
            <li><span style={{ color: 'var(--muted)' }}>Delhi (Select CITYWALK & Vegas)</span></li>
            <li><span style={{ color: 'var(--muted)' }}>Mumbai (Phoenix Palladium & Kurla)</span></li>
          </ul>
        </div>
        <div className="footerLinksCol">
          <h4>Premium Formats</h4>
          <ul>
            <li><span style={{ color: 'var(--muted)' }}>IMAX with Laser 70mm</span></li>
            <li><span style={{ color: 'var(--muted)' }}>Dolby Atmos 64-Channel</span></li>
            <li><span style={{ color: 'var(--muted)' }}>4DX Environmental FX</span></li>
          </ul>
        </div>
        <div className="footerLinksCol">
          <h4>Booking & Safety</h4>
          <ul>
            <li><Link to="/bookings">My Active Bookings</Link></li>
            <li><Link to="/movies">Now Showing Directory</Link></li>
            <li><span style={{ color: 'var(--muted)' }}>Verified Screenings Only</span></li>
          </ul>
        </div>
      </div>
      <div className="footerBottom">
        <span>© {new Date().getFullYear()} CineBook. All rights reserved. Real database screening schedules.</span>
        <span>Cinema Ticket Booking Platform</span>
      </div>
    </footer>
  );
}

// Navigation Bar
function Navbar() {
  const [q, setQ] = useState('');
  const [previewResults, setPreviewResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  const [currentCity, setCurrentCity] = useState(getStoredCity());
  const [cityList, setCityList] = useState(['Chandigarh', 'Delhi', 'Mumbai']);
  const [showCityMenu, setShowCityMenu] = useState(false);
  const [theme, setTheme] = useState(getStoredTheme());

  const [currentUser, setCurrentUser] = useState(getStoredUser());
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authNotice, setAuthNotice] = useState('');
  const [authSuccessCallback, setAuthSuccessCallback] = useState(null);

  const dropdownRef = useRef(null);
  const cityRef = useRef(null);
  const userMenuRef = useRef(null);
  const nav = useNavigate();
  const location = useLocation();

  useEffect(() => {
    axios.get(`${API}/movies/cities`)
      .then((r) => r.data?.length && setCityList(r.data))
      .catch(() => {});

    const handleCityChange = () => setCurrentCity(getStoredCity());
    const handleThemeChange = () => setTheme(getStoredTheme());
    const handleUserChange = () => setCurrentUser(getStoredUser());

    const handleOpenAuth = (e) => {
      setAuthNotice(e.detail?.message || 'Sign in required to proceed with booking.');
      setAuthSuccessCallback(() => e.detail?.onSuccess);
      setShowAuthModal(true);
    };

    window.addEventListener('city_changed', handleCityChange);
    window.addEventListener('theme_changed', handleThemeChange);
    window.addEventListener('user_changed', handleUserChange);
    window.addEventListener('open_auth_modal', handleOpenAuth);

    return () => {
      window.removeEventListener('city_changed', handleCityChange);
      window.removeEventListener('theme_changed', handleThemeChange);
      window.removeEventListener('user_changed', handleUserChange);
      window.removeEventListener('open_auth_modal', handleOpenAuth);
    };
  }, []);

  const toggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    setStoredTheme(nextTheme);
    setTheme(nextTheme);
  };

  // Debounced search preview
  useEffect(() => {
    if (!q || q.trim().length < 2) {
      setPreviewResults([]);
      setShowDropdown(false);
      return;
    }

    setIsSearching(true);
    const timer = setTimeout(() => {
      axios.get(`${API}/movies/search`, { params: { query: q.trim(), page: 1 } })
        .then((res) => {
          setPreviewResults(res.data?.results?.slice(0, 5) || []);
          setShowDropdown(true);
        })
        .catch(() => setPreviewResults([]))
        .finally(() => setIsSearching(false));
    }, 350);

    return () => clearTimeout(timer);
  }, [q]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
      if (cityRef.current && !cityRef.current.contains(event.target)) {
        setShowCityMenu(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) {
        setShowUserMenu(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearchSubmit = (e) => {
    if (e.key === 'Enter' && q.trim()) {
      setShowDropdown(false);
      nav(`/search?q=${encodeURIComponent(q.trim())}`);
    }
  };

  const handleSelectCity = (c) => {
    setStoredCity(c);
    setCurrentCity(c);
    setShowCityMenu(false);
  };

  const handleSignOut = () => {
    logoutUser();
    setShowUserMenu(false);
  };

  return (
    <>
      <nav>
        <Link to="/" className="brand">
          <span>🎬</span> CineBook
        </Link>

        <div className="navSearchContainer" ref={dropdownRef}>
          <div className="search">
            <Search size={18} />
            <input
              placeholder="Search movies by title, actor, genre..."
              value={q}
              onChange={(e) => setQ(e.target.value)}
              onKeyDown={handleSearchSubmit}
              onFocus={() => previewResults.length > 0 && setShowDropdown(true)}
            />
            {q && (
              <button className="searchClear" onClick={() => { setQ(''); setShowDropdown(false); }}>
                <X size={15} />
              </button>
            )}
          </div>

          {showDropdown && (
            <div className="searchDropdown">
              {isSearching ? (
                <div className="searchDropdownEmpty">Searching movies...</div>
              ) : previewResults.length > 0 ? (
                <>
                  {previewResults.map((m) => (
                    <Link
                      to={`/movie/${m.id}`}
                      key={m.id}
                      className="searchDropdownItem"
                      onClick={() => setShowDropdown(false)}
                    >
                      <ImageWithFallback src={m.posterUrl} alt={m.title} />
                      <div className="searchDropdownInfo">
                        <h4>{m.title}</h4>
                        <div className="searchDropdownMeta">
                          <span>⭐ {m.rating}</span>
                          <span>{m.releaseDate ? m.releaseDate.split('-')[0] : ''}</span>
                          <span>{m.genres?.[0] || ''}</span>
                        </div>
                      </div>
                    </Link>
                  ))}
                  <Link
                    to={`/search?q=${encodeURIComponent(q.trim())}`}
                    className="searchDropdownAll"
                    onClick={() => setShowDropdown(false)}
                  >
                    View all results for "{q}" →
                  </Link>
                </>
              ) : (
                <div className="searchDropdownEmpty">No movies found for "{q}"</div>
              )}
            </div>
          )}
        </div>

        <div className="navright">
          {/* City Selector */}
          <div className="citySelector" ref={cityRef}>
            <button className="cityBtn" onClick={() => setShowCityMenu(!showCityMenu)}>
              <MapPin size={15} color="var(--accent)" /> {currentCity} ⌄
            </button>
            {showCityMenu && (
              <div className="cityDropdown">
                {cityList.map((c) => (
                  <div
                    key={c}
                    className={`cityDropdownItem ${currentCity.toLowerCase() === c.toLowerCase() ? 'active' : ''}`}
                    onClick={() => handleSelectCity(c)}
                  >
                    <span>{c}</span>
                    {currentCity.toLowerCase() === c.toLowerCase() && <Check size={14} />}
                  </div>
                ))}
              </div>
            )}
          </div>

          <Link to="/movies" className={`navlink ${location.pathname === '/movies' ? 'active' : ''}`}>
            Now Showing
          </Link>

          <Link to="/bookings" className={`navlink ${location.pathname === '/bookings' ? 'active' : ''}`}>
            <Ticket size={16} /> My Bookings
          </Link>

          {/* Theme Toggle Button */}
          <button
            className="themeToggleBtn"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            aria-label="Toggle Light/Dark Theme"
          >
            {theme === 'dark' ? <Sun size={17} color="#f3c969" /> : <Moon size={17} color="#4f46e5" />}
          </button>

          {/* User Account Controls */}
          {currentUser ? (
            <div className="userAccountMenuWrapper" ref={userMenuRef}>
              <button
                className="userAccountBtn"
                onClick={() => setShowUserMenu(!showUserMenu)}
                title={`Signed in as ${currentUser.name}`}
              >
                <div className="navUserAvatar" style={{ background: currentUser.avatarBg || '#ff474c' }}>
                  {currentUser.initials || currentUser.name?.[0] || 'U'}
                </div>
                <span className="navUserName">{currentUser.name?.split(' ')[0] || 'User'}</span>
                <span className="userMenuChevron">⌄</span>
              </button>

              {showUserMenu && (
                <div className="userMenuDropdown">
                  <div className="userMenuHeader">
                    <div className="userMenuHeaderAvatar" style={{ background: currentUser.avatarBg || '#ff474c' }}>
                      {currentUser.initials || currentUser.name?.[0] || 'U'}
                    </div>
                    <div className="userMenuHeaderDetails">
                      <div className="userMenuHeaderName">{currentUser.name}</div>
                      <div className="userMenuHeaderEmail">{currentUser.email}</div>
                      <span className="userMenuVerifiedBadge">● LOGGED IN</span>
                    </div>
                  </div>

                  <div className="userMenuDivider" />

                  <div className="userMenuActions">
                    <Link
                      to="/bookings"
                      className="userMenuActionBtn"
                      onClick={() => setShowUserMenu(false)}
                    >
                      <Ticket size={15} /> My Bookings
                    </Link>
                    <Link
                      to="/movies"
                      className="userMenuActionBtn"
                      onClick={() => setShowUserMenu(false)}
                    >
                      <Film size={15} /> Now Showing Directory
                    </Link>
                    <button
                      className="userMenuActionBtn logout"
                      onClick={handleSignOut}
                    >
                      <XCircle size={15} /> Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <button
              className="primary navSignInBtn"
              onClick={() => {
                setAuthNotice('');
                setAuthSuccessCallback(null);
                setShowAuthModal(true);
              }}
            >
              <User size={15} /> Sign In
            </button>
          )}
        </div>
      </nav>

      <AuthModal
        isOpen={showAuthModal}
        noticeMessage={authNotice}
        onSuccess={(user) => {
          if (authSuccessCallback) {
            authSuccessCallback(user);
            setAuthSuccessCallback(null);
          }
        }}
        onClose={() => {
          setShowAuthModal(false);
          setAuthNotice('');
          setAuthSuccessCallback(null);
        }}
      />
    </>
  );
}

function SkeletonCards({ count = 6 }) {
  return (
    <div className="grid">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="carouselCard" style={{ animation: 'pulse 1.5s infinite ease-in-out' }}>
          <div className="poster" style={{ background: '#1c1c24' }} />
          <div className="ct">
            <div style={{ height: 16, background: '#22222d', borderRadius: 4, width: '80%' }} />
            <div style={{ height: 12, background: '#22222d', borderRadius: 4, width: '50%', marginTop: 8 }} />
          </div>
        </div>
      ))}
    </div>
  );
}

// ==========================================
// 1. HOME PAGE
// Strict Order:
// 1. Currently Showing (city-based)
// 2. Book Tickets (Quick booking bar)
// 3. Coming Soon (strictly future release dates)
// 4. Featured Movies (only with active shows in city)
// 5. Offers and Promotions
// ==========================================

function Home() {
  const [city, setCity] = useState(getStoredCity());
  const [nowShowing, setNowShowing] = useState([]);
  const [comingSoon, setComingSoon] = useState([]);
  const [heroIndex, setHeroIndex] = useState(0);
  const [activeTrailerKey, setActiveTrailerKey] = useState(null);
  const [loading, setLoading] = useState(true);
  const [copiedCode, setCopiedCode] = useState(null);

  const copyCoupon = (code) => {
    navigator.clipboard?.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2500);
  };

  // Quick book bar state
  const [selectedMovieId, setSelectedMovieId] = useState('');
  const [selectedDate, setSelectedDate] = useState('TODAY');
  const [theaters, setTheaters] = useState([]);
  const [selectedTheaterId, setSelectedTheaterId] = useState('');

  const nav = useNavigate();

  useEffect(() => {
    const handleCityChange = () => setCity(getStoredCity());
    window.addEventListener('city_changed', handleCityChange);
    return () => window.removeEventListener('city_changed', handleCityChange);
  }, []);

  useEffect(() => {
    setLoading(true);
    setHeroIndex(0);
    Promise.allSettled([
      axios.get(`${API}/movies/now-showing`, { params: { city } }),
      axios.get(`${API}/movies/coming-soon`),
      axios.get(`${API}/shows/theaters`, { params: { city } }),
    ]).then(([nowRes, soonRes, thRes]) => {
      const nowList = nowRes.value?.data || [];
      const soonList = soonRes.value?.data || [];
      const thList = thRes.value?.data || [];

      setNowShowing(nowList);
      setComingSoon(soonList);
      setTheaters(thList);

      if (nowList.length > 0) {
        setSelectedMovieId(nowList[0].id);
      } else {
        setSelectedMovieId('');
      }

      if (thList.length > 0) {
        setSelectedTheaterId(thList[0].id);
      }

      setLoading(false);
    });
  }, [city]);

  const heroMovie = nowShowing[heroIndex] || null;
  const trailerKey = heroMovie ? extractYoutubeKey(heroMovie.trailerUrl) : null;

  const handleQuickBook = (e) => {
    e.preventDefault();
    if (selectedMovieId) {
      requireAuth('Sign in required to proceed with quick booking', () => {
        nav(`/show/${selectedMovieId}`);
      });
    }
  };

  return (
    <>
      <Navbar />
      <main>
        {/* 1. CURRENTLY SHOWING HERO */}
        {heroMovie ? (
          <section
            className="heroBanner"
            style={{ backgroundImage: `url(${heroMovie.backdropUrl})` }}
          >
            <div className="heroBackdropOverlay" />
            <div className="heroBannerContent">
              <div className="heroBannerMeta">
                <span className="ratingPill">
                  <Star size={14} fill="currentColor" /> {heroMovie.rating || 8.5} / 10
                </span>
                <span className="cityStatusBadge">
                  ● NOW SHOWING IN {city.toUpperCase()}
                </span>
                {heroMovie.durationMinutes ? (
                  <span className="metaPill">
                    <Clock size={12} style={{ marginRight: 4 }} />
                    {formatRuntime(heroMovie.durationMinutes)}
                  </span>
                ) : null}
                {heroMovie.language ? (
                  <span className="metaPill">
                    <Globe size={12} style={{ marginRight: 4 }} />
                    {heroMovie.language}
                  </span>
                ) : null}
              </div>

              <h1>{heroMovie.title}</h1>
              <p className="heroOverview">{heroMovie.overview}</p>

              <div className="detailsBadgesRow">
                {heroMovie.genres?.map((g) => (
                  <span key={g} className="genreBadge">
                    <Tag size={11} style={{ marginRight: 3 }} /> {g}
                  </span>
                ))}
              </div>

              <div className="heroActions">
                <button
                  type="button"
                  className="primary big"
                  onClick={() => {
                    requireAuth(`Sign in required to book tickets for ${heroMovie.title}`, () => {
                      nav(`/show/${heroMovie.id}`);
                    });
                  }}
                >
                  <Ticket size={18} /> Book Tickets Now <ChevronRight size={18} />
                </button>
                {trailerKey && (
                  <button className="btnPlay" onClick={() => setActiveTrailerKey(trailerKey)}>
                    <Play size={18} fill="currentColor" /> Watch Trailer
                  </button>
                )}
                <Link to={`/movie/${heroMovie.id}`} className="btnOutline">
                  View Details
                </Link>
              </div>

              {/* Multiple movies switcher dots in hero */}
              {nowShowing.length > 1 && (
                <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                  {nowShowing.map((m, idx) => (
                    <button
                      key={m.id}
                      onClick={() => setHeroIndex(idx)}
                      style={{
                        width: idx === heroIndex ? 24 : 8,
                        height: 8,
                        borderRadius: 4,
                        background: idx === heroIndex ? 'var(--accent)' : 'rgba(255,255,255,0.3)',
                        border: 0,
                        padding: 0,
                        transition: 'all 0.25s ease'
                      }}
                      title={m.title}
                    />
                  ))}
                </div>
              )}
            </div>
          </section>
        ) : (
          <div style={{ padding: '60px 6vw 20px', textAlign: 'center' }}>
            <span className="eyebrow">LOCATION: {city.toUpperCase()}</span>
            <h1 style={{ fontSize: 36, margin: '10px 0' }}>No Active Shows in {city}</h1>
            <p style={{ color: 'var(--muted)', maxWidth: 600, margin: '10px auto 25px' }}>
              There are currently no scheduled screenings in {city}. Please choose another city (such as Chandigarh, Delhi, or Mumbai) from the top-right city selector.
            </p>
          </div>
        )}

        {/* 1. Currently Showing Carousel */}
        <MovieCarousel
          title={`Currently Showing in ${city}`}
          subtitle="TICKETS AVAILABLE TODAY"
          items={nowShowing}
          linkTo="/movies"
          emptyMessage={`No movies currently scheduled for screening in ${city}.`}
        />

        {/* 2. BOOK TICKETS (Quick Booking Bar) */}
        {nowShowing.length > 0 && (
          <section className="quickBookSection">
            <div className="quickBookContainer">
              <div className="quickBookHeader">
                <Ticket size={22} color="var(--accent)" />
                <div>
                  <h3 style={{ margin: 0 }}>Quick Ticket Booking</h3>
                  <p style={{ margin: 0, color: 'var(--muted)', fontSize: 13 }}>Select movie, date & theater in {city} to reserve seats instantly</p>
                </div>
              </div>

              <form className="quickBookGrid" onSubmit={handleQuickBook}>
                <div className="quickBookField">
                  <label>1. Select Movie</label>
                  <select
                    className="quickBookSelect"
                    value={selectedMovieId}
                    onChange={(e) => setSelectedMovieId(e.target.value)}
                  >
                    {nowShowing.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.title} ({m.language || 'English'})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="quickBookField">
                  <label>2. Select Date</label>
                  <select
                    className="quickBookSelect"
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                  >
                    <option value="TODAY">Today (03 Nov)</option>
                    <option value="TOMORROW">Tomorrow (04 Nov)</option>
                    <option value="FRI 06">Friday (06 Nov)</option>
                    <option value="SAT 07">Saturday (07 Nov)</option>
                  </select>
                </div>

                <div className="quickBookField">
                  <label>3. Select Theater</label>
                  <select
                    className="quickBookSelect"
                    value={selectedTheaterId}
                    onChange={(e) => setSelectedTheaterId(e.target.value)}
                  >
                    {theaters.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                  </select>
                </div>

                <button type="submit" className="primary" style={{ padding: '12px 24px', height: 46, whiteSpace: 'nowrap' }}>
                  Select Seats <ChevronRight size={17} />
                </button>
              </form>
            </div>
          </section>
        )}

        {/* 3. COMING SOON (Strictly future release dates) */}
        <MovieCarousel
          title="Coming Soon"
          subtitle="RELEASING IN FUTURE"
          items={comingSoon}
          emptyMessage="No upcoming releases scheduled at this moment."
        />

        {/* 4. FEATURED MOVIES (Only movies with active shows in selected city) */}
        {nowShowing.length > 0 && (
          <section style={{ margin: '50px 0' }}>
            <div className="carouselHeader">
              <div className="carouselTitleGroup">
                <span className="eyebrow">VERIFIED SCREENINGS</span>
                <h2>Featured Movies in {city}</h2>
              </div>
            </div>

            <div className="featuredGrid">
              {nowShowing.map((m) => (
                <div key={m.id} className="featuredCard">
                  <div className="featuredMedia">
                    <ImageWithFallback src={m.backdropUrl} alt={m.title} fallbackType="backdrop" />
                    <div className="featuredOverlay" />
                  </div>
                  <div className="featuredBody">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="ratingPill" style={{ padding: '2px 8px', fontSize: 12 }}>
                        <Star size={12} fill="currentColor" /> {m.rating || 8.5}
                      </span>
                      <span className="genreBadge">{m.genres?.[0] || 'Cinema'}</span>
                    </div>
                    <h3>{m.title}</h3>
                    <p>{m.overview}</p>
                    
                    <div className="showtimePills">
                      <span className="showtimePill highlight">11:00 AM (IMAX)</span>
                      <span className="showtimePill">02:30 PM</span>
                      <span className="showtimePill">06:45 PM</span>
                      <span className="showtimePill">10:15 PM</span>
                    </div>

                    <div style={{ marginTop: 'auto', paddingTop: 14, display: 'flex', gap: 10 }}>
                      <button
                        type="button"
                        className="primary"
                        style={{ flex: 1, justifyContent: 'center' }}
                        onClick={() => {
                          requireAuth(`Sign in required to book seats for ${m.title}`, () => {
                            nav(`/show/${m.id}`);
                          });
                        }}
                      >
                        Book Seats <Ticket size={16} />
                      </button>
                      <Link to={`/movie/${m.id}`} className="btnOutline" style={{ padding: '10px 16px' }}>
                        Details
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* 5. OFFERS AND PROMOTIONS */}
        <section style={{ margin: '50px 0' }}>
          <div className="carouselHeader">
            <div className="carouselTitleGroup">
              <span className="eyebrow">EXCLUSIVE DEALS</span>
              <h2>Cinema Offers & Promotions</h2>
            </div>
          </div>

          <div className="offersGrid">
            <div className="offerCard">
              <h4>Weekend Cinema Pass</h4>
              <p>Flat ₹100 instant discount when you book 2 or more tickets for any evening show.</p>
              <span
                className={`offerCode ${copiedCode === 'CINEWEEKEND' ? 'copied' : ''}`}
                onClick={() => copyCoupon('CINEWEEKEND')}
                title="Click to copy coupon code"
              >
                {copiedCode === 'CINEWEEKEND' ? <><Check size={13} /> COPIED!</> : 'CINEWEEKEND 📋'}
              </span>
            </div>

            <div className="offerCard">
              <h4>IMAX & Dolby Upgrade</h4>
              <p>Complimentary gourmet salted caramel popcorn on every premium format ticket.</p>
              <span
                className={`offerCode ${copiedCode === 'PREMIUMIMAX' ? 'copied' : ''}`}
                onClick={() => copyCoupon('PREMIUMIMAX')}
                title="Click to copy coupon code"
              >
                {copiedCode === 'PREMIUMIMAX' ? <><Check size={13} /> COPIED!</> : 'PREMIUMIMAX 📋'}
              </span>
            </div>

            <div className="offerCard">
              <h4>HDFC & ICICI Bank Offer</h4>
              <p>Enjoy 20% cashback up to ₹250 on debit & credit cards on movie purchases.</p>
              <span
                className={`offerCode ${copiedCode === 'BANK20CB' ? 'copied' : ''}`}
                onClick={() => copyCoupon('BANK20CB')}
                title="Click to copy coupon code"
              >
                {copiedCode === 'BANK20CB' ? <><Check size={13} /> COPIED!</> : 'BANK20CB 📋'}
              </span>
            </div>

            <div className="offerCard">
              <h4>Student Special Weekdays</h4>
              <p>Special student tickets starting from ₹150 for Monday to Thursday matinée shows.</p>
              <span
                className={`offerCode ${copiedCode === 'STUDENTPASS' ? 'copied' : ''}`}
                onClick={() => copyCoupon('STUDENTPASS')}
                title="Click to copy coupon code"
              >
                {copiedCode === 'STUDENTPASS' ? <><Check size={13} /> COPIED!</> : 'STUDENTPASS 📋'}
              </span>
            </div>
          </div>
        </section>

        {/* SITE FOOTER */}
        <Footer />
      </main>

      {activeTrailerKey && (
        <TrailerModal videoKey={activeTrailerKey} onClose={() => setActiveTrailerKey(null)} />
      )}
    </>
  );
}

// ==========================================
// ALL MOVIES (Now Showing in Selected City)
// ==========================================

function MoviesPage() {
  const [city, setCity] = useState(getStoredCity());
  const [tab, setTab] = useState('now_showing');
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleCityChange = () => setCity(getStoredCity());
    window.addEventListener('city_changed', handleCityChange);
    return () => window.removeEventListener('city_changed', handleCityChange);
  }, []);

  useEffect(() => {
    setLoading(true);
    const endpoint = tab === 'now_showing'
      ? `${API}/movies/now-showing?city=${encodeURIComponent(city)}`
      : `${API}/movies/coming-soon`;

    axios.get(endpoint)
      .then((res) => setMovies(res.data || []))
      .catch(() => setMovies([]))
      .finally(() => setLoading(false));
  }, [city, tab]);

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="sectionHead" style={{ padding: 0 }}>
          <div>
            <span className="eyebrow">CINEMA DIRECTORY</span>
            <h1>{tab === 'now_showing' ? `Now Showing in ${city}` : 'Coming Soon to Cinemas'}</h1>
            <p>
              {tab === 'now_showing'
                ? `Only movies with active, verified shows in ${city} are displayed.`
                : 'Upcoming theatrical releases strictly scheduled for future dates.'}
            </p>
          </div>

          <div className="timePills">
            <button
              className={`timePill ${tab === 'now_showing' ? 'active' : ''}`}
              onClick={() => setTab('now_showing')}
            >
              Now Showing ({city})
            </button>
            <button
              className={`timePill ${tab === 'coming_soon' ? 'active' : ''}`}
              onClick={() => setTab('coming_soon')}
            >
              Coming Soon
            </button>
          </div>
        </div>

        {loading ? (
          <SkeletonCards count={6} />
        ) : movies.length > 0 ? (
          <div className="grid">
            {movies.map((m) => (
              <MovieCard m={m} key={m.id} />
            ))}
          </div>
        ) : (
          <div className="searchStateBox">
            <Film size={44} color="#777" style={{ margin: '0 auto 12px' }} />
            <h3>No movies found</h3>
            <p>
              {tab === 'now_showing'
                ? `There are currently no active shows in ${city}. Try selecting another city from the top navigation.`
                : 'No upcoming releases listed currently.'}
            </p>
          </div>
        )}
      </main>
    </>
  );
}

// ==========================================
// SEARCH PAGE
// ==========================================

function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [searchTerm, setSearchTerm] = useState(query);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setSearchTerm(query);
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    setError(null);

    axios.get(`${API}/movies/search`, { params: { query: query.trim() } })
      .then((res) => {
        setResults(res.data?.results || []);
      })
      .catch(() => {
        setError('Failed to fetch search results. Please try again.');
        setResults([]);
      })
      .finally(() => setLoading(false));
  }, [query]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchTerm.trim()) {
      setSearchParams({ q: searchTerm.trim() });
    }
  };

  return (
    <>
      <Navbar />
      <main className="page">
        <div className="searchPageHeader">
          <span className="eyebrow">SEARCH RESULTS</span>
          <h1>{query ? `Results for "${query}"` : 'Search Movies'}</h1>
        </div>

        <form onSubmit={handleSearchSubmit} style={{ maxWidth: 600, marginBottom: 35 }}>
          <div className="filterSearch">
            <Search size={18} />
            <input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search movie title, actor, genre..."
            />
            {searchTerm && (
              <button type="button" className="searchClear" onClick={() => setSearchTerm('')}>
                <X size={15} />
              </button>
            )}
          </div>
        </form>

        {loading ? (
          <SkeletonCards count={6} />
        ) : error ? (
          <div className="searchStateBox">
            <AlertCircle size={44} color="#ff5b5f" style={{ margin: '0 auto 14px' }} />
            <h3>Unable to load results</h3>
            <p>{error}</p>
          </div>
        ) : results.length > 0 ? (
          <div className="grid">
            {results.map((m) => (
              <MovieCard m={m} key={m.id} />
            ))}
          </div>
        ) : query ? (
          <div className="searchStateBox">
            <Film size={44} color="#777" style={{ margin: '0 auto 12px' }} />
            <h3>No movies found</h3>
            <p>We couldn't find any movies matching "{query}".</p>
          </div>
        ) : null}
      </main>
    </>
  );
}

// ==========================================
// MOVIE DETAILS PAGE
// ==========================================

function MovieDetails() {
  const { id } = useParams();
  const nav = useNavigate();
  const [city] = useState(getStoredCity());
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTrailerKey, setActiveTrailerKey] = useState(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });

    axios.get(`${API}/movies/${id}`, { params: { city } })
      .then((res) => {
        setMovie(res.data);
      })
      .catch(() => {
        setError('Failed to load movie details.');
      })
      .finally(() => setLoading(false));
  }, [id, city]);

  if (loading) {
    return (
      <>
        <Navbar />
        <main className="page" style={{ textAlign: 'center', padding: '120px 20px' }}>
          <div className="spinner" />
          <h2 style={{ marginTop: 20 }}>Loading movie details...</h2>
        </main>
      </>
    );
  }

  if (error || !movie) {
    return (
      <>
        <Navbar />
        <main className="page">
          <div className="searchStateBox">
            <AlertCircle size={48} color="#ff5b5f" style={{ margin: '0 auto 16px' }} />
            <h3>Movie Not Found</h3>
            <p>{error || "We couldn't retrieve the requested movie."}</p>
            <button className="primary" onClick={() => nav(-1)}>
              <ArrowLeft size={16} /> Go Back
            </button>
          </div>
        </main>
      </>
    );
  }

  const trailerKey = extractYoutubeKey(movie.primaryTrailer || movie.trailerUrl);
  const isBookable = movie.bookingAvailable;

  return (
    <>
      <Navbar />
      <main>
        <div
          className="detailsHeroWrapper"
          style={{ backgroundImage: `url(${movie.backdropUrl})` }}
        >
          <div className="detailsHeroBackdrop" />
          <div className="detailsHeroContainer">
            <div className="detailsPosterCard">
              <ImageWithFallback src={movie.posterUrl} alt={movie.title} />
            </div>

            <div className="detailsHeroInfo">
              <button className="back" onClick={() => nav(-1)}>
                <ArrowLeft size={17} /> Back
              </button>

              <div className="detailsBadgesRow">
                <span className="ratingPill">
                  <Star size={14} fill="currentColor" /> {movie.rating || 'N/A'} / 10
                </span>

                {isBookable ? (
                  <span className="genreBadge" style={{ background: 'rgba(16, 185, 129, 0.16)', color: 'var(--emerald)', borderColor: 'rgba(16, 185, 129, 0.35)' }}>
                    ● Shows Available in {city}
                  </span>
                ) : (
                  <span className="genreBadge neutral">
                    No active shows in {city}
                  </span>
                )}

                {movie.durationMinutes && (
                  <span className="genreBadge neutral">
                    <Clock size={12} style={{ marginRight: 4 }} />
                    {formatRuntime(movie.durationMinutes)}
                  </span>
                )}
                {movie.language && (
                  <span className="genreBadge neutral">{movie.language}</span>
                )}
                {movie.releaseDate && (
                  <span className="genreBadge neutral">
                    <Calendar size={12} style={{ marginRight: 4 }} />
                    {formatDate(movie.releaseDate)}
                  </span>
                )}
              </div>

              <h1>{movie.title}</h1>

              <div className="detailsBadgesRow" style={{ marginTop: 4 }}>
                {movie.genres?.map((g) => (
                  <span key={g} className="genreBadge">
                    {g}
                  </span>
                ))}
              </div>

              <p className="detailsOverview">{movie.overview}</p>

              <div className="heroActions" style={{ marginTop: 16 }}>
                {isBookable ? (
                  <button
                    type="button"
                    className="primary big"
                    onClick={() => {
                      requireAuth(`Sign in required to book tickets for ${movie.title}`, () => {
                        nav(`/show/${movie.id}`);
                      });
                    }}
                  >
                    <Ticket size={18} /> Book Tickets in {city} <ChevronRight size={18} />
                  </button>
                ) : (
                  <button className="primary big" disabled style={{ opacity: 0.5, cursor: 'not-allowed' }}>
                    No Shows in {city}
                  </button>
                )}

                {trailerKey ? (
                  <button className="btnPlay" onClick={() => setActiveTrailerKey(trailerKey)}>
                    <Play size={18} fill="currentColor" /> Watch Trailer
                  </button>
                ) : null}
              </div>
            </div>
          </div>
        </div>

        <div className="section" style={{ maxWidth: 1100, margin: '0 auto', padding: '40px 6vw' }}>
          {/* Cast */}
          {movie.cast && movie.cast.length > 0 && (
            <div style={{ marginBottom: 40 }}>
              <span className="eyebrow">CAST</span>
              <h2 style={{ fontSize: 24, margin: '8px 0 16px', color: '#fff' }}>Top Billed Cast</h2>
              <div className="castGrid">
                {movie.cast.map((c) => (
                  <div key={c.id || c.name} className="castCard">
                    {c.profileUrl ? (
                      <ImageWithFallback src={c.profileUrl} alt={c.name} className="castPhoto" fallbackType="profile" />
                    ) : (
                      <div className="castPhotoPlaceholder"><User size={32} /></div>
                    )}
                    <div className="castInfo">
                      <h4 className="castName">{c.name}</h4>
                      <p className="castRole">{c.character}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Videos */}
          {movie.videos && movie.videos.length > 0 && (
            <div style={{ marginBottom: 40 }}>
              <span className="eyebrow">MEDIA</span>
              <h2 style={{ fontSize: 24, margin: '8px 0 16px', color: '#fff' }}>Trailers & Videos</h2>
              <div className="videosGrid">
                {movie.videos.map((v) => {
                  const key = extractYoutubeKey(v);
                  return (
                    <div key={v.id || key} className="videoCard" onClick={() => key && setActiveTrailerKey(key)}>
                      <div className="videoThumbnail">
                        <img src={`https://img.youtube.com/vi/${key}/hqdefault.jpg`} alt={v.name} />
                        <div className="videoPlayOverlay">
                          <div className="playCircle"><Play size={20} fill="currentColor" /></div>
                        </div>
                      </div>
                      <div className="videoInfo">
                        <h4>{v.name}</h4>
                        <span className="videoBadge">{v.type}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </main>

      {activeTrailerKey && (
        <TrailerModal videoKey={activeTrailerKey} onClose={() => setActiveTrailerKey(null)} />
      )}
    </>
  );
}

// ==========================================
// SHOW & BOOKING FLOW
// ==========================================

function Show() {
  const { id } = useParams();
  const nav = useNavigate();
  const [city] = useState(getStoredCity());
  const [movie, setMovie] = useState(null);
  const [showsData, setShowsData] = useState(null);

  const dateTabs = Array.from({ length: 5 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    const dayLabel = i === 0 ? 'TODAY' : i === 1 ? 'TOMORROW' : d.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase();
    const dateSub = d.toLocaleDateString('en-US', { day: '2-digit', month: 'short' }).toUpperCase();
    const isoDate = d.toISOString().split('T')[0];
    return { key: isoDate, dayLabel, dateSub };
  });

  const [selectedDateKey, setSelectedDateKey] = useState(dateTabs[0]?.key);

  useEffect(() => {
    if (!id) return;
    axios.get(`${API}/movies/${id}`, { params: { city } }).then((r) => setMovie(r.data)).catch(() => {});
    axios.get(`${API}/shows/movie/${id}`, { params: { city } }).then((r) => setShowsData(r.data)).catch(() => {});
  }, [id, city]);

  const theaters = showsData?.theaters || [];

  return (
    <>
      <Navbar />
      <main className="page narrow">
        <button className="back dark" onClick={() => nav(-1)}>
          <ArrowLeft size={17} /> Movie details
        </button>

        <div className="showTitle">
          <span className="eyebrow">CHOOSE SHOWTIME • {city.toUpperCase()}</span>
          <h1>{movie?.title || 'Book Tickets'}</h1>
          <p>{movie?.genres?.join(' • ')} • {movie?.language}</p>
        </div>

        <div className="dates">
          {dateTabs.map((dt) => (
            <button
              key={dt.key}
              className={selectedDateKey === dt.key ? 'active' : ''}
              onClick={() => setSelectedDateKey(dt.key)}
            >
              {dt.dayLabel}
              <b>{dt.dateSub}</b>
            </button>
          ))}
        </div>

        {theaters.length > 0 ? (
          theaters.map((th) => {
            const dateFilteredTimes = th.times?.filter(t => !t.date || t.date === selectedDateKey);
            const displayedTimes = (dateFilteredTimes && dateFilteredTimes.length > 0) ? dateFilteredTimes : th.times;

            return (
              <div key={th.id} style={{ marginBottom: 25, background: '#111116', padding: 20, borderRadius: 14, border: '1px solid var(--line)' }}>
                <div className="theater" style={{ margin: 0, padding: '0 0 16px' }}>
                  <div>
                    <h3>{th.name}</h3>
                    <p>{th.city} • {th.address}</p>
                  </div>
                  <span className="tag">₹{th.startingPrice} onwards</span>
                </div>

                <div className="times" style={{ marginTop: 12 }}>
                  {displayedTimes?.map((t) => (
                    <button
                      key={t.showId}
                      type="button"
                      onClick={() => {
                        requireAuth(`Sign in required to select seats for ${movie?.title || 'this show'}`, () => {
                          nav(`/seats/${id}?showId=${t.showId}&time=${encodeURIComponent(t.time)}&theater=${encodeURIComponent(th.name)}&date=${encodeURIComponent(t.date || selectedDateKey)}`);
                        });
                      }}
                    >
                      {t.time}
                      <small>{t.format} • ₹{t.price}</small>
                    </button>
                  ))}
                </div>
              </div>
            );
          })
        ) : (
          <div className="searchStateBox">
            <Film size={44} color="#777" style={{ margin: '0 auto 12px' }} />
            <h3>No shows scheduled</h3>
            <p>There are no shows available for this title in {city}. Please try another city.</p>
          </div>
        )}
      </main>
    </>
  );
}

function Seats() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const showId = searchParams.get('showId') || 'demo';
  const showTime = searchParams.get('time') || '10:00 PM';
  const theaterName = searchParams.get('theater') || 'INOX • Elante Mall';

  const nav = useNavigate();
  const [selected, setSelected] = useState([]);
  const [seats, setSeats] = useState([]);
  const [movie, setMovie] = useState(null);

  useEffect(() => {
    if (id) {
      axios.get(`${API}/movies/${id}`).then((r) => setMovie(r.data)).catch(() => {});
    }
    axios.get(`${API}/shows/${showId}/seats`)
      .then((r) => setSeats(r.data))
      .catch(() => {
        const a = [];
        ['A', 'B', 'C', 'D', 'E', 'F'].forEach((row) => {
          for (let n = 1; n <= 10; n++) {
            a.push({
              id: row + n,
              label: row + n,
              type: row === 'F' ? 'RECLINER' : row === 'E' ? 'PLATINUM' : 'GOLD',
              price: row === 'F' ? 500 : row === 'E' ? 350 : 250,
              status: (row === 'C' && n === 4) || (row === 'D' && n === 7) ? 'BOOKED' : 'AVAILABLE'
            });
          }
        });
        setSeats(a);
      });
  }, [id, showId]);

  const toggle = (s) =>
    s.status !== 'BOOKED' &&
    setSelected((x) => (x.includes(s.id) ? x.filter((i) => i !== s.id) : [...x, s.id]));

  const total = selected.reduce((sum, seatId) => sum + (seats.find((s) => s.id === seatId)?.price || 0), 0);

  return (
    <>
      <Navbar />
      <main className="seatPage">
        <div className="seatTop">
          <button className="back dark" onClick={() => nav(-1)}>
            <ArrowLeft size={17} /> Showtimes
          </button>
          <div>
            <span className="eyebrow">{theaterName.toUpperCase()}</span>
            <h1 style={{ fontSize: 28, margin: '6px 0', color: '#fff' }}>Select your seats</h1>
            <p style={{ color: 'var(--muted)', margin: 0 }}>{movie?.title || 'Selected Movie'} • Today • {showTime}</p>
          </div>
        </div>

        <div className="screen">SCREEN THIS WAY</div>

        <div className="seatMap">
          {['A', 'B', 'C', 'D', 'E', 'F'].map((row) => (
            <div className="seatRow" key={row}>
              <label>{row}</label>
              {seats
                .filter((s) => s.label.startsWith(row))
                .map((s) => (
                  <button
                    key={s.id}
                    title={`${s.type} - ₹${s.price}`}
                    onClick={() => toggle(s)}
                    className={'seat ' + s.status.toLowerCase() + (selected.includes(s.id) ? ' selected' : '')}
                  >
                    {s.id.replace(row, '')}
                  </button>
                ))}
            </div>
          ))}
        </div>

        <div className="legend">
          <span><i className="available" /> Available</span>
          <span><i className="selectedDot" /> Selected</span>
          <span><i className="booked" /> Booked</span>
        </div>

        <div className="summary">
          <div>
            <small>SELECTED SEATS</small>
            <strong>{selected.length ? selected.join(', ') : 'None selected'}</strong>
          </div>
          <div>
            <small>TOTAL</small>
            <strong>₹{total}</strong>
          </div>
          <button
            disabled={!selected.length}
            className="primary"
            onClick={() => {
              requireAuth('Sign in required to proceed to checkout', () => {
                nav('/checkout', {
                  state: {
                    selected,
                    total,
                    movieId: id,
                    movieTitle: movie?.title || 'Movie',
                    theater: theaterName,
                    time: showTime,
                    showId: showId !== 'demo' ? showId : undefined
                  }
                });
              });
            }}
          >
            Continue <ChevronRight size={18} />
          </button>
        </div>
      </main>
    </>
  );
}

function Checkout() {
  const nav = useNavigate();
  const location = useLocation();
  const state = location.state || {};
  const [done, setDone] = useState(false);
  const [bookingCode, setBookingCode] = useState('');

  const movieTitle = state.movieTitle || 'Selected Movie';
  const totalAmount = state.total || 0;
  const convenienceFee = 40;
  const gst = Math.round(totalAmount * 0.05);
  const grandTotal = totalAmount + convenienceFee + gst;

  const handlePay = () => {
    requireAuth('Sign in required to confirm tickets and complete payment', (currentUser) => {
      const code = 'CBK-' + Math.floor(Math.random() * 900000 + 100000);
      setBookingCode(code);

      // Persist to backend database for logged-in user
      axios.post(`${API}/bookings`, {
        email: currentUser.email,
        name: currentUser.name,
        movieTitle,
        theaterName: state.theater || 'INOX • Elante Mall',
        showTime: state.time || 'Today • 10:00 PM',
        showId: state.showId,
        seats: state.selected || [],
        amount: grandTotal
      }).catch((err) => console.warn('Backend booking save notice:', err));

      // Also persist to current user's local cache
      const userEmail = currentUser.email;
      const pastBookings = JSON.parse(localStorage.getItem(`cinebook_bookings_${userEmail}`) || '[]');
      pastBookings.unshift({
        code,
        title: movieTitle,
        theater: state.theater || 'INOX • Elante Mall',
        time: state.time || 'Today • 10:00 PM',
        seats: state.selected || [],
        amount: grandTotal,
        status: 'CONFIRMED',
        date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
      });
      localStorage.setItem(`cinebook_bookings_${userEmail}`, JSON.stringify(pastBookings));

      setDone(true);
    });
  };

  if (done) {
    return (
      <>
        <Navbar />
        <main className="success">
          <div className="check">
            <Check size={34} />
          </div>
          <span className="eyebrow">BOOKING CONFIRMED</span>
          <h1 style={{ fontSize: 36, margin: '10px 0' }}>Your seats are secured!</h1>
          <p style={{ color: 'var(--muted)' }}>
            Your ticket reference code is <b style={{ color: '#fff' }}>{bookingCode}</b>.
          </p>
          <div className="ticket">
            <Ticket />
            <div>
              <b>{movieTitle}</b>
              <span>{state.theater || 'INOX • Elante Mall'}</span>
              <span>{state.time || 'Today • 10:00 PM'}</span>
              <span>Seats: {state.selected?.join(', ')}</span>
              <span>Total Paid: ₹{grandTotal}</span>
            </div>
          </div>
          <Link to="/bookings" className="primary">
            View My Bookings
          </Link>
        </main>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <main className="checkout page">
        <button className="back dark" onClick={() => nav(-1)}>
          <ArrowLeft size={17} /> Seats
        </button>
        <div className="checkoutGrid">
          <div>
            <span className="eyebrow">CHECKOUT</span>
            <h1 style={{ fontSize: 32, margin: '8px 0 20px' }}>Complete your booking</h1>
            <div className="payBox">
              <h3 style={{ margin: '0 0 16px', color: '#fff' }}>Select payment method</h3>
              <button className="pay active">
                UPI / QR Code <span>Instant</span>
              </button>
              <button className="pay">Credit / Debit Card</button>
              <button className="pay">Net Banking</button>
            </div>
          </div>

          <aside className="bill">
            <h3 style={{ margin: '0 0 16px', color: '#fff' }}>Order summary</h3>
            <p>
              {movieTitle} <span>₹{totalAmount}</span>
            </p>
            <p>
              Convenience fee <span>₹{convenienceFee}</span>
            </p>
            <p>
              GST (5%) <span>₹{gst}</span>
            </p>
            <hr />
            <h2>
              Total <span>₹{grandTotal}</span>
            </h2>
            <button className="primary full" onClick={handlePay}>
              Pay securely (₹{grandTotal})
            </button>
            <small>Test mode — no real money is charged.</small>
          </aside>
        </div>
      </main>
    </>
  );
}

function Bookings() {
  const [currentUser, setCurrentUser] = useState(getStoredUser());
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL' | 'ACTIVE' | 'CANCELLED'
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [notification, setNotification] = useState(null);

  const loadUserBookings = (user) => {
    if (!user || !user.email) {
      setBookings([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    axios.get(`${API}/bookings`, { params: { email: user.email } })
      .then((res) => {
        if (Array.isArray(res.data) && res.data.length > 0) {
          const normalized = res.data.map((b) => ({
            code: b.bookingCode || b.code,
            title: b.movieTitle || b.title,
            theater: b.theaterName || b.theater,
            time: b.showTime || b.time || b.date,
            date: b.bookingDate || b.date,
            seats: typeof b.seats === 'string' ? b.seats.split(',').map(s => s.trim()) : (b.seats || []),
            amount: b.totalAmount || b.amount,
            status: b.status || 'CONFIRMED',
            cancelledAt: b.cancelledAt,
            refundAmount: b.refundAmount || b.amount,
            refundStatus: b.refundStatus,
            posterUrl: b.posterUrl
          }));
          setBookings(normalized);
        } else {
          // If backend returns empty, check localStorage cache for this user
          const localList = JSON.parse(localStorage.getItem(`cinebook_bookings_${user.email}`) || '[]');
          setBookings(localList);
        }
      })
      .catch(() => {
        // Fallback to local cache for offline resilience
        const localList = JSON.parse(localStorage.getItem(`cinebook_bookings_${user.email}`) || '[]');
        setBookings(localList);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const handleUserChange = () => {
      const u = getStoredUser();
      setCurrentUser(u);
      loadUserBookings(u);
    };

    window.addEventListener('user_changed', handleUserChange);
    loadUserBookings(currentUser);

    return () => window.removeEventListener('user_changed', handleUserChange);
  }, []);

  const handleConfirmCancel = async () => {
    if (!cancellingBooking) return;
    const nowStr = new Date().toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });

    try {
      await axios.post(`${API}/bookings/${cancellingBooking.code}/cancel`);
    } catch (e) {
      console.warn('Backend cancel call failed, updating locally:', e);
    }

    const updated = bookings.map((b) => {
      if (b.code === cancellingBooking.code) {
        return {
          ...b,
          status: 'CANCELLED',
          cancelledAt: nowStr,
          refundAmount: b.amount,
          refundStatus: 'REFUND_INITIATED'
        };
      }
      return b;
    });

    if (currentUser?.email) {
      localStorage.setItem(`cinebook_bookings_${currentUser.email}`, JSON.stringify(updated));
    }
    setBookings(updated);
    setNotification({
      code: cancellingBooking.code,
      title: cancellingBooking.title,
      amount: cancellingBooking.amount
    });
    setCancellingBooking(null);
  };

  const activeBookings = bookings.filter((b) => b.status !== 'CANCELLED');
  const cancelledBookings = bookings.filter((b) => b.status === 'CANCELLED');

  const displayedBookings = activeTab === 'ACTIVE'
    ? activeBookings
    : activeTab === 'CANCELLED'
    ? cancelledBookings
    : bookings;

  return (
    <>
      <Navbar />
      <main className="page narrow">
        {/* User Account Scope Banner */}
        {currentUser ? (
          <div className="bookingsUserBanner">
            <div className="bookingsUserLeft">
              <div className="bookingsUserAvatar" style={{ background: currentUser?.avatarBg || '#ff474c' }}>
                {currentUser?.initials || currentUser?.name?.[0] || 'U'}
              </div>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <h3 style={{ margin: 0, fontSize: 18, color: 'var(--text)' }}>
                    {currentUser?.name}
                  </h3>
                  <span className="userVerifiedPill">
                    <Check size={11} /> LOGGED IN
                  </span>
                </div>
                <p style={{ margin: '3px 0 0', color: 'var(--muted)', fontSize: 13 }}>
                  {currentUser?.email} • {bookings.length} {bookings.length === 1 ? 'ticket' : 'tickets'} in your account
                </p>
              </div>
            </div>
            <button className="btnSwitchAccount" onClick={logoutUser} title="Sign Out of account">
              <XCircle size={14} /> Sign Out
            </button>
          </div>
        ) : (
          <div className="bookingsUserBanner signedOutBanner">
            <div className="bookingsUserLeft">
              <div className="bookingsUserAvatar" style={{ background: '#3b3b4f' }}>
                <User size={22} color="#fff" />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: 18, color: 'var(--text)' }}>
                  You are currently Signed Out
                </h3>
                <p style={{ margin: '3px 0 0', color: 'var(--muted)', fontSize: 13 }}>
                  Sign in with your account to view your reserved tickets and booking history.
                </p>
              </div>
            </div>
            <button className="primary" onClick={() => requireAuth('Sign in to view your personal bookings')}>
              <User size={14} /> Sign In
            </button>
          </div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <span className="eyebrow">YOUR DIGITAL TICKETS</span>
            <h1 style={{ fontSize: 32, margin: '6px 0 16px' }}>My Bookings</h1>
          </div>
        </div>

        {/* Cancellation Success Toast */}
        {notification && (
          <div className="cancelToast">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <CheckCircle2 size={20} />
              <span>
                Booking <b>{notification.code}</b> ({notification.title}) cancelled. 100% refund of <b>₹{notification.amount}</b> initiated to your payment source.
              </span>
            </div>
            <button onClick={() => setNotification(null)} title="Dismiss">
              <X size={16} />
            </button>
          </div>
        )}

        {/* Filter Tabs */}
        {currentUser && bookings.length > 0 && (
          <div className="bookingFilterTabs">
            <button
              className={`bookingFilterTab ${activeTab === 'ALL' ? 'active' : ''}`}
              onClick={() => setActiveTab('ALL')}
            >
              All Tickets <span className="countBadge">{bookings.length}</span>
            </button>
            <button
              className={`bookingFilterTab ${activeTab === 'ACTIVE' ? 'active' : ''}`}
              onClick={() => setActiveTab('ACTIVE')}
            >
              Active <span className="countBadge">{activeBookings.length}</span>
            </button>
            <button
              className={`bookingFilterTab ${activeTab === 'CANCELLED' ? 'active' : ''}`}
              onClick={() => setActiveTab('CANCELLED')}
            >
              Cancelled <span className="countBadge">{cancelledBookings.length}</span>
            </button>
          </div>
        )}

        {!currentUser ? (
          <div className="empty">
            <User size={48} color="var(--accent)" />
            <h2 style={{ color: 'var(--text)', margin: '14px 0 8px' }}>
              Sign In to View Your Bookings
            </h2>
            <p style={{ color: 'var(--muted)', marginBottom: 20 }}>
              CineBook keeps bookings strictly separated for each individual user account. Please sign in to view your active and cancelled tickets.
            </p>
            <button className="primary big" onClick={() => requireAuth('Sign in to view your personal bookings')}>
              <User size={16} /> Sign In Now
            </button>
          </div>
        ) : loading ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: 'var(--muted)' }}>
            <RefreshCw size={28} className="spin" style={{ margin: '0 auto 12px' }} />
            <p>Loading your personal tickets...</p>
          </div>
        ) : displayedBookings.length > 0 ? (
          <div className="bookingsList">
            {displayedBookings.map((b) => {
              const isCancelled = b.status === 'CANCELLED';
              return (
                <div key={b.code} className={`bookingCard ${isCancelled ? 'isCancelled' : ''}`}>
                  <div className="bookingMain">
                    {b.posterUrl && (
                      <div className="bookingPoster">
                        <ImageWithFallback src={b.posterUrl} alt={b.title} />
                      </div>
                    )}
                    <div className="bookingInfo">
                      <h3>{b.title}</h3>
                      <div className="bookingMeta">
                        <span>{b.theater}</span>
                        <span>•</span>
                        <span>{b.time}</span>
                        <span>•</span>
                        <span>Code: <b>{b.code}</b></span>
                        {b.date && (
                          <>
                            <span>•</span>
                            <span>Booked on {b.date}</span>
                          </>
                        )}
                      </div>
                      <div className="seatChips">
                        {Array.isArray(b.seats) ? b.seats.map((s) => (
                          <span key={s} className="chip">
                            {s}
                          </span>
                        )) : (
                          <span className="chip">{b.seats}</span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="bookingRight">
                    {isCancelled ? (
                      <>
                        <span className="badge cancelled">
                          <XCircle size={13} /> Cancelled
                        </span>
                        <span className="bookingPrice" style={{ fontSize: 18, color: '#888', textDecoration: 'line-through' }}>
                          ₹{b.amount}
                        </span>
                        <span className="cancelledRefundNote">
                          Refund ₹{b.refundAmount || b.amount} initiated
                          {b.cancelledAt ? ` on ${b.cancelledAt}` : ''}
                        </span>
                      </>
                    ) : (
                      <>
                        <span className="badge confirmed">
                          <Check size={13} /> Confirmed
                        </span>
                        <span className="bookingPrice" style={{ fontSize: 20, fontWeight: 700 }}>
                          ₹{b.amount}
                        </span>
                        <button
                          className="btnCancelTicket"
                          onClick={() => setCancellingBooking(b)}
                          title="Cancel this movie ticket"
                        >
                          <XCircle size={14} /> Cancel Ticket
                        </button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="empty">
            <Ticket size={42} />
            <h2 style={{ color: 'var(--text)', margin: '14px 0 8px' }}>
              {activeTab === 'ACTIVE'
                ? 'No active bookings'
                : activeTab === 'CANCELLED'
                ? 'No cancelled bookings'
                : 'No bookings yet for this account'}
            </h2>
            <p style={{ color: 'var(--muted)', marginBottom: 20 }}>
              {activeTab === 'ACTIVE'
                ? 'All your past reservations have been cancelled or concluded.'
                : activeTab === 'CANCELLED'
                ? 'You do not have any cancelled tickets.'
                : `Logged in as ${currentUser?.name || 'User'}. Book your next movie screening to see tickets here!`}
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link className="primary" to="/movies">
                Explore Now Showing
              </Link>
            </div>
          </div>
        )}
      </main>

      {/* Cancellation Confirmation Modal */}
      {cancellingBooking && (
        <div className="cancelModalOverlay" onClick={() => setCancellingBooking(null)}>
          <div className="cancelModalCard" onClick={(e) => e.stopPropagation()}>
            <div className="cancelModalHeader">
              <div className="cancelModalIcon">
                <AlertTriangle size={24} />
              </div>
              <div>
                <h3>Cancel Ticket Booking?</h3>
                <p>Are you sure you want to cancel this ticket? Your reserved seats will be released immediately.</p>
              </div>
            </div>

            <div className="cancelTicketDetails">
              <div className="cancelDetailRow">
                <span>Movie</span>
                <strong>{cancellingBooking.title}</strong>
              </div>
              <div className="cancelDetailRow">
                <span>Theater</span>
                <strong>{cancellingBooking.theater}</strong>
              </div>
              <div className="cancelDetailRow">
                <span>Show Time</span>
                <strong>{cancellingBooking.time}</strong>
              </div>
              <div className="cancelDetailRow">
                <span>Reference Code</span>
                <strong style={{ letterSpacing: '0.5px' }}>{cancellingBooking.code}</strong>
              </div>
              <div className="cancelDetailRow">
                <span>Reserved Seats</span>
                <strong>{Array.isArray(cancellingBooking.seats) ? cancellingBooking.seats.join(', ') : cancellingBooking.seats || 'None'}</strong>
              </div>
              <div className="cancelDetailRow highlight">
                <span>Refund Amount (100%)</span>
                <strong>₹{cancellingBooking.amount}</strong>
              </div>
            </div>

            <div className="cancelPolicyNotice">
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <div>
                <b>Cancellation Policy:</b> 100% full refund of ₹{cancellingBooking.amount} will be credited back to your original payment method within 3 to 5 business days. Cancellation is irreversible.
              </div>
            </div>

            <div className="cancelModalActions">
              <button className="btnKeepBooking" onClick={() => setCancellingBooking(null)}>
                Keep My Booking
              </button>
              <button className="btnConfirmCancel" onClick={handleConfirmCancel}>
                <XCircle size={16} /> Yes, Cancel Ticket
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ==========================================
// APP ROUTER
// ==========================================

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/movies" element={<MoviesPage />} />
      <Route path="/search" element={<SearchPage />} />
      <Route path="/movie/:id" element={<MovieDetails />} />
      <Route path="/show/:id" element={<Show />} />
      <Route path="/seats/:id" element={<Seats />} />
      <Route path="/checkout" element={<Checkout />} />
      <Route path="/bookings" element={<Bookings />} />
    </Routes>
  );
}

createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <App />
  </BrowserRouter>
);
