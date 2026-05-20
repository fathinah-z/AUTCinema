# Project 2 – Derby Database Layer
## Cinema Booking System – COMP603/ENSE600

---

## Overview

This package adds the Apache Derby Embedded database layer to the Project 1
Cinema Booking System.  The design follows **clean layered architecture**:

```
GUI / Controller
      │
  Service Layer  (unchanged from Project 1)
      │
  Repository interfaces  (unchanged from Project 1)
      │
  DB Repository Adapters  ◄── NEW: bridge between P1 interfaces and DAOs
      │
  DAO interfaces          ◄── NEW
      │
  Derby DAO implementations ◄── NEW
      │
  Apache Derby Embedded DB
```

---

## Design Patterns Applied

| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `DatabaseManager` | One shared Connection; Derby Embedded must not be opened by multiple JVMs |
| **Abstract Factory** | `DAOFactory` | Creates all DAO instances; swapping to a mock DB or H2 only touches this class |
| **DAO (Data Access Object)** | All `*DAO` interfaces + `Derby*DAO` classes | Separates SQL from business logic; satisfies the assignment database requirement |
| **Adapter** | `Db*Repository` classes | Bridges the unchanged P1 `*Repository` interfaces to the new DAO layer without modifying any service code |

---

## File Inventory

### `cinemaapp/db/`
| File | Purpose |
|---|---|
| `DatabaseManager.java` | **Singleton** — holds and vends the single Derby `Connection`; handles graceful shutdown |
| `DatabaseInitialiser.java` | Creates all tables on first run; seeds movies, screens, seats, showtimes |

### `cinemaapp/dao/` (interfaces)
| File | Purpose |
|---|---|
| `AccountDAO.java` | Authenticate / register user accounts |
| `MovieDAO.java` | Full CRUD for movies |
| `ScreenDAO.java` | Find screen + load its seats |
| `ShowtimeDAO.java` | Find showtimes by ID or movie |
| `ShowSeatDAO.java` | Find/update seat status per showtime; reset reserved seats |
| `BookingDAO.java` | Transactional save/delete of bookings + items |
| `DAOFactory.java` | **Abstract Factory** — vends all DAO instances |

### `cinemaapp/dao/derby/` (implementations)
| File | Purpose |
|---|---|
| `DerbyAccountDAO.java` | SQL for Account table |
| `DerbyMovieDAO.java` | SQL for Movie table |
| `DerbyScreenDAO.java` | SQL for Screen + Seat tables |
| `DerbyShowtimeDAO.java` | SQL for Showtime table |
| `DerbyShowSeatDAO.java` | SQL for ShowSeat table |
| `DerbyBookingDAO.java` | SQL for Booking + BookingItem tables (transactional) |

### `cinemaapp/repository/` (adapters — NEW DB-backed versions)
| File | Purpose |
|---|---|
| `DbMovieRepository.java` | Implements `MovieRepository` via `MovieDAO` |
| `DbScreenRepository.java` | Implements `ScreenRepository` via `ScreenDAO` |
| `DbShowtimeRepository.java` | Implements `ShowtimeRepository` via `ShowtimeDAO` |
| `DbShowSeatRepository.java` | Implements `ShowSeatRepository` via `ShowSeatDAO` |
| `DbBookingRepository.java` | Implements `BookingRepository` via `BookingDAO`; exposes `setCurrentUsername()` for session |

### `cinemaapp/model/`
| File | Purpose |
|---|---|
| `Account.java` | **NEW** — represents a logged-in user |

### `test/`
| File | Purpose |
|---|---|
| `DerbyDAOTest.java` | JUnit 4 — 10 test cases covering all DAOs |

---

## Integration Guide (adding to the NetBeans Project 2 project)

### 1. Add Derby library
In NetBeans: **right-click project → Properties → Libraries → Add Library → Java DB (Derby)**
This adds `derby.jar` (embedded driver) to the classpath.

### 2. Add JUnit 4 library
**Libraries → Add Library → JUnit 4**

### 3. Copy source files
Copy these packages into your `src/` folder alongside the existing `cinemaapp` package:
```
cinemaapp/db/
cinemaapp/dao/
cinemaapp/model/Account.java
cinemaapp/repository/Db*Repository.java
```

### 4. Wire up in your application entry point
Replace the `File*Repository` instantiations with the DB-backed equivalents:

```java
// In your GUI application entry point (e.g. CinemaGUIApp.java)

// 1. Start DB
DatabaseManager  dbManager  = DatabaseManager.getInstance();
new DatabaseInitialiser(dbManager).initialise();

// 2. Create DAO factory
DAOFactory factory = new DAOFactory(dbManager);

// 3. Create DB-backed repositories (drop-in replacements for File* repos)
DbBookingRepository  bookingRepo  = new DbBookingRepository(factory.getBookingDAO());
DbMovieRepository    movieRepo    = new DbMovieRepository(factory.getMovieDAO());
DbScreenRepository   screenRepo   = new DbScreenRepository(factory.getScreenDAO());
DbShowtimeRepository showtimeRepo = new DbShowtimeRepository(factory.getShowtimeDAO());
DbShowSeatRepository showSeatRepo = new DbShowSeatRepository(factory.getShowSeatDAO());

// 4. After login, set username on booking repo
AccountDAO accountDAO = factory.getAccountDAO();
Account user = accountDAO.authenticate(username, password);
if (user != null) {
    bookingRepo.setCurrentUsername(user.getUsername());
}

// 5. Services are unchanged – pass the new repos in
PricingService       pricingService    = new PricingService();
BookingCodeGenerator codeGenerator     = new BookingCodeGenerator();
BrowsingService      browsingService   = new BrowsingService(movieRepo, showtimeRepo, showSeatRepo);
MakeBookingService   makeBookingService= new MakeBookingService(
        bookingRepo, screenRepo, showtimeRepo, showSeatRepo, movieRepo,
        codeGenerator, pricingService);
CancelBookingService cancelService     = new CancelBookingService(
        bookingRepo, showSeatRepo, showtimeRepo);

// 6. Shut down Derby when the app closes
Runtime.getRuntime().addShutdownHook(new Thread(dbManager::shutdown));
```

### 5. Run the tests
Place `test/DerbyDAOTest.java` under your project's `test/` source root.
Run via **NetBeans → right-click test file → Test File**.

---

## Database Schema

Matches the provided ERD exactly:

```
Account      (username PK, password)
Movie        (movieId PK, title, rating, description, runtime)
Screen       (screenId PK)
Seat         (seatId PK, row, number, nearAisle, isAccessible, screenId FK)
Showtime     (showtimeId PK, dateTime, basePrice, screenId FK, movieId FK)
ShowSeat     (seatId FK + showtimeId FK composite PK, seatStatus)
Booking      (bookingCode PK, bookingDate, totalPrice, username FK)
BookingItem  (bookingCode FK + seatId FK composite PK, itemPrice, attendeeType)
```

The database is created automatically on first run as `CinemaBookingDB/`
in the NetBeans project working directory.  No manual setup is required.

---

## Seed Data

On first run, `DatabaseInitialiser` inserts:
- **2 accounts**: `admin / admin123`, `guest / guest`
- **5 movies**: Interstellar, The Dark Knight, Parasite, Toy Story, Inception
- **2 screens**: S01 (rows A-E, 10 seats) and S02 (rows A-D, 8 seats)
- **6 showtimes** across June 2026
- **All ShowSeat rows** pre-set to `AVAILABLE`
