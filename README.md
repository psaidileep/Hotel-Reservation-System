# 🏨 Hotel Reservation System  

A **Java console application** for booking hotel rooms with:  
✔️ Room availability checks  
✔️ Date validation (prevents double-booking)  
✔️ SQLite database integration  
✔️ Receipt generation  

## 🚀 How to Run  
1. **Prerequisites**:  
   - Java 8+  
   - SQLite JDBC driver (included in `lib/`)  

2. **Steps**:  
   ```bash
   # Compile
   javac -cp .;lib/sqlite-jdbc-3.36.0.3.jar HotelReservationSystem.java
   
   # Run
   java -cp .;lib/sqlite-jdbc-3.36.0.3.jar HotelReservationSystem



## ✨ Features
- ✅ Room booking with check-in/check-out dates  
- 🚫 Double-booking prevention  
- 💰 Automatic cost calculation  
- 🧾 Booking receipt generation  
- 💾 SQLite database persistence  

## 🛠️ Tech Stack
- **Core**: Java (OOP principles)  
- **Database**: SQLite (with JDBC driver)  
- **Tools**: VS Code, Git  

## 📦 Project Structure

HotelReservationSystem/
├── src/
│ └── HotelReservationSystem.java # Main application
├── lib/
│ └── sqlite-jdbc-3.36.0.3.jar # Database driver
└── README.md # This file