package src;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HotelReservationSystem {
    // Database configuration
    private static final String DB_URL = "jdbc:sqlite:hotel.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    // Main entities
    static class Room {
        private int roomId;
        private String roomNumber;
        private String roomType;
        private double pricePerNight;
        private boolean isAvailable;
        
        public Room(int roomId, String roomNumber, String roomType, double pricePerNight, boolean isAvailable) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.pricePerNight = pricePerNight;
            this.isAvailable = isAvailable;
        }

        // Getters
        public int getRoomId() { return roomId; }
        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public double getPricePerNight() { return pricePerNight; }
        public boolean isAvailable() { return isAvailable; }
    }

    static class Reservation {
        private int reservationId;
        private int roomId;
        private String guestName;
        private String guestEmail;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private double totalPrice;
        private String status;
        
        public Reservation(int roomId, String guestName, String guestEmail, 
                         LocalDate checkInDate, LocalDate checkOutDate, double totalPrice) {
            this.roomId = roomId;
            this.guestName = guestName;
            this.guestEmail = guestEmail;
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
            this.totalPrice = totalPrice;
            this.status = "Confirmed";
        }

        // Getters and setters
        public int getReservationId() { return reservationId; }
        public void setReservationId(int reservationId) { this.reservationId = reservationId; }
        public int getRoomId() { return roomId; }
        public String getGuestName() { return guestName; }
        public String getGuestEmail() { return guestEmail; }
        public LocalDate getCheckInDate() { return checkInDate; }
        public LocalDate getCheckOutDate() { return checkOutDate; }
        public double getTotalPrice() { return totalPrice; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    static class Payment {
        private int paymentId;
        private int reservationId;
        private double amount;
        private LocalDate paymentDate;
        private String paymentMethod;
        
        public Payment(int reservationId, double amount, String paymentMethod) {
            this.reservationId = reservationId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDate = LocalDate.now();
        }

        // Getters and setters
        public int getPaymentId() { return paymentId; }
        public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
        public int getReservationId() { return reservationId; }
        public double getAmount() { return amount; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        public String getPaymentMethod() { return paymentMethod; }
    }

    // Database manager
    static class DatabaseManager {
        public static void initializeDatabase() {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                
                // Create tables
                stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "room_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "room_number TEXT NOT NULL UNIQUE," +
                    "room_type TEXT NOT NULL," +
                    "price_per_night REAL NOT NULL," +
                    "is_available BOOLEAN NOT NULL DEFAULT 1)");
                
                stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "room_id INTEGER NOT NULL," +
                    "guest_name TEXT NOT NULL," +
                    "guest_email TEXT NOT NULL," +
                    "check_in_date TEXT NOT NULL," +
                    "check_out_date TEXT NOT NULL," +
                    "total_price REAL NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY (room_id) REFERENCES rooms(room_id))");
                
                stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "payment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "reservation_id INTEGER NOT NULL," +
                    "amount REAL NOT NULL," +
                    "payment_date TEXT NOT NULL," +
                    "payment_method TEXT NOT NULL," +
                    "FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id))");
                
                // Insert sample data if empty
                insertSampleRooms();
                
            } catch (SQLException e) {
                System.out.println("Database initialization failed: " + e.getMessage());
            }
        }
        
        private static void insertSampleRooms() {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms")) {
                
                if (rs.getInt(1) == 0) {
                    String[] roomTypes = {"Standard", "Deluxe", "Suite"};
                    double[] prices = {100.0, 150.0, 250.0};
                    
                    for (int i = 1; i <= 10; i++) {
                        String roomType = roomTypes[i % 3];
                        double price = prices[i % 3];
                        stmt.executeUpdate(String.format(
                            "INSERT INTO rooms (room_number, room_type, price_per_night) " +
                            "VALUES ('%d', '%s', %.2f)", i, roomType, price));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error inserting sample rooms: " + e.getMessage());
            }
        }
        
        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL);
        }
    }

    // Service classes
    static class RoomService {
        public List<Room> getAllAvailableRooms() {
            List<Room> rooms = new ArrayList<>();
            String sql = "SELECT * FROM rooms WHERE is_available = 1";
            
            try (Connection conn = DatabaseManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    rooms.add(new Room(
                        rs.getInt("room_id"),
                        rs.getString("room_number"),
                        rs.getString("room_type"),
                        rs.getDouble("price_per_night"),
                        rs.getBoolean("is_available")));
                }
            } catch (SQLException e) {
                System.out.println("Error fetching rooms: " + e.getMessage());
            }
            return rooms;
        }
        
        public List<Room> searchAvailableRooms(String roomType, LocalDate checkIn, LocalDate checkOut) {
            List<Room> availableRooms = new ArrayList<>();
            String sql = "SELECT r.* FROM rooms r WHERE r.room_type = ? AND r.is_available = 1 " +
                         "AND NOT EXISTS (SELECT 1 FROM reservations res WHERE res.room_id = r.room_id " +
                         "AND res.status = 'Confirmed' AND res.check_out_date > ? AND res.check_in_date < ?)";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, roomType);
                pstmt.setString(2, checkIn.format(DATE_FORMATTER));
                pstmt.setString(3, checkOut.format(DATE_FORMATTER));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        availableRooms.add(new Room(
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("room_type"),
                            rs.getDouble("price_per_night"),
                            rs.getBoolean("is_available")));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error searching rooms: " + e.getMessage());
            }
            return availableRooms;
        }
        
        public boolean updateRoomAvailability(int roomId, boolean isAvailable) {
            String sql = "UPDATE rooms SET is_available = ? WHERE room_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setBoolean(1, isAvailable);
                pstmt.setInt(2, roomId);
                
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Error updating room availability: " + e.getMessage());
                return false;
            }
        }
        
        public Room findRoomByNumber(String roomNumber) {
            String sql = "SELECT * FROM rooms WHERE room_number = ? AND is_available = 1";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, roomNumber);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Room(
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("room_type"),
                            rs.getDouble("price_per_night"),
                            rs.getBoolean("is_available"));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error finding room: " + e.getMessage());
            }
            return null;
        }
    }

    static class ReservationService {
        public boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut) {
            String sql = "SELECT COUNT(*) FROM reservations WHERE room_id = ? AND status = 'Confirmed' " +
                         "AND ((check_in_date < ? AND check_out_date > ?))";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, roomId);
                pstmt.setString(2, checkOut.format(DATE_FORMATTER));
                pstmt.setString(3, checkIn.format(DATE_FORMATTER));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) == 0;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error checking room availability: " + e.getMessage());
            }
            return false;
        }
        
        public double calculateTotalCost(int roomId, LocalDate checkIn, LocalDate checkOut) {
            String sql = "SELECT price_per_night FROM rooms WHERE room_id = ?";
            double pricePerNight = 0;
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, roomId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        pricePerNight = rs.getDouble("price_per_night");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error getting room price: " + e.getMessage());
            }
            
            long days = ChronoUnit.DAYS.between(checkIn, checkOut);
            return days * pricePerNight;
        }
        
        public void generateReceipt(Reservation reservation, Payment payment) {
            System.out.println("\n=== BOOKING RECEIPT ===");
            System.out.println("Reservation ID: " + reservation.getReservationId());
            System.out.println("Guest Name: " + reservation.getGuestName());
            System.out.println("Room ID: " + reservation.getRoomId());
            System.out.println("Check-in: " + reservation.getCheckInDate());
            System.out.println("Check-out: " + reservation.getCheckOutDate());
            System.out.println("Total Nights: " + 
                ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate()));
            System.out.printf("Total Cost: $%.2f%n", reservation.getTotalPrice());
            System.out.println("Payment Method: " + payment.getPaymentMethod());
            System.out.println("Payment Date: " + payment.getPaymentDate());
            System.out.println("Status: " + reservation.getStatus());
            System.out.println("=========================");
        }
        
        public Reservation makeReservation(int roomId, String guestName, String guestEmail, 
                                         LocalDate checkIn, LocalDate checkOut, double pricePerNight) {
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            double totalPrice = nights * pricePerNight;
            Reservation reservation = new Reservation(roomId, guestName, guestEmail, checkIn, checkOut, totalPrice);
            
            String sql = "INSERT INTO reservations (room_id, guest_name, guest_email, " +
                         "check_in_date, check_out_date, total_price, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                pstmt.setInt(1, roomId);
                pstmt.setString(2, guestName);
                pstmt.setString(3, guestEmail);
                pstmt.setString(4, checkIn.format(DATE_FORMATTER));
                pstmt.setString(5, checkOut.format(DATE_FORMATTER));
                pstmt.setDouble(6, totalPrice);
                pstmt.setString(7, reservation.getStatus());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) return null;
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reservation.setReservationId(generatedKeys.getInt(1));
                    }
                }
                
                RoomService roomService = new RoomService();
                roomService.updateRoomAvailability(roomId, false);
                
                return reservation;
            } catch (SQLException e) {
                System.out.println("Error making reservation: " + e.getMessage());
                return null;
            }
        }
        
        public boolean cancelReservation(int reservationId) {
            int roomId = -1;
            String getRoomSql = "SELECT room_id FROM reservations WHERE reservation_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(getRoomSql)) {
                
                pstmt.setInt(1, reservationId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        roomId = rs.getInt("room_id");
                    } else {
                        return false;
                    }
                }
                
                String updateReservationSql = "UPDATE reservations SET status = 'Cancelled' WHERE reservation_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateReservationSql)) {
                    updateStmt.setInt(1, reservationId);
                    if (updateStmt.executeUpdate() == 0) return false;
                }
                
                if (roomId != -1) {
                    RoomService roomService = new RoomService();
                    roomService.updateRoomAvailability(roomId, true);
                }
                
                return true;
            } catch (SQLException e) {
                System.out.println("Error cancelling reservation: " + e.getMessage());
                return false;
            }
        }
        
        public Reservation getReservationDetails(int reservationId) {
            String sql = "SELECT * FROM reservations WHERE reservation_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, reservationId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Reservation reservation = new Reservation(
                            rs.getInt("room_id"),
                            rs.getString("guest_name"),
                            rs.getString("guest_email"),
                            LocalDate.parse(rs.getString("check_in_date"), DATE_FORMATTER),
                            LocalDate.parse(rs.getString("check_out_date"), DATE_FORMATTER),
                            rs.getDouble("total_price"));
                        reservation.setReservationId(rs.getInt("reservation_id"));
                        reservation.setStatus(rs.getString("status"));
                        return reservation;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error fetching reservation: " + e.getMessage());
            } catch (DateTimeParseException e) {
                System.out.println("Error parsing reservation dates: " + e.getMessage());
            }
            return null;
        }
    }

    static class PaymentService {
        public boolean processPayment(int reservationId, double amount, String paymentMethod) {
            Payment payment = new Payment(reservationId, amount, paymentMethod);
            
            String sql = "INSERT INTO payments (reservation_id, amount, payment_date, payment_method) " +
                         "VALUES (?, ?, ?, ?)";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, payment.getReservationId());
                pstmt.setDouble(2, payment.getAmount());
                pstmt.setString(3, payment.getPaymentDate().format(DATE_FORMATTER));
                pstmt.setString(4, payment.getPaymentMethod());
                
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Error processing payment: " + e.getMessage());
                return false;
            }
        }
        
        public Payment getPaymentDetails(int reservationId) {
            String sql = "SELECT * FROM payments WHERE reservation_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, reservationId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Payment payment = new Payment(
                            rs.getInt("reservation_id"),
                            rs.getDouble("amount"),
                            rs.getString("payment_method"));
                        payment.setPaymentId(rs.getInt("payment_id"));
                        
                        // Safe date parsing with error handling
                        String dateStr = rs.getString("payment_date");
                        try {
                            payment.setPaymentDate(dateStr != null ? 
                                LocalDate.parse(dateStr, DATE_FORMATTER) : 
                                LocalDate.now());
                        } catch (DateTimeParseException e) {
                            System.out.println("Invalid payment date format, using current date");
                            payment.setPaymentDate(LocalDate.now());
                        }
                        
                        return payment;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Error fetching payment: " + e.getMessage());
            }
            return null;
        }
    }

    // Main application
    private static Scanner scanner = new Scanner(System.in);
    private static RoomService roomService = new RoomService();
    private static ReservationService reservationService = new ReservationService();
    private static PaymentService paymentService = new PaymentService();
    
    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        showMainMenu();
    }
    
    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== Hotel Reservation System ===");
            System.out.println("1. Search Available Rooms");
            System.out.println("2. Make a Reservation");
            System.out.println("3. View/Cancel Reservation");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            switch (choice) {
                case 1: searchRooms(); break;
                case 2: makeReservation(); break;
                case 3: manageReservation(); break;
                case 4: 
                    System.out.println("Thank you for using our system. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private static void searchRooms() {
        System.out.println("\n=== Search Available Rooms ===");
        System.out.print("Enter room type (Standard/Deluxe/Suite or leave blank for all): ");
        String roomType = scanner.nextLine();
        
        LocalDate checkIn = getDateInput("Enter check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = getDateInput("Enter check-out date (YYYY-MM-DD): ");
        
        List<Room> availableRooms;
        if (roomType.isEmpty()) {
            availableRooms = roomService.getAllAvailableRooms();
        } else {
            availableRooms = roomService.searchAvailableRooms(roomType, checkIn, checkOut);
        }
        
        if (availableRooms.isEmpty()) {
            System.out.println("No available rooms found for your criteria.");
        } else {
            System.out.println("\nAvailable Rooms:");
            System.out.printf("%-10s %-10s %-15s %-10s%n", 
                "Room No.", "Type", "Price/Night", "Available");
            for (Room room : availableRooms) {
                System.out.printf("%-10s %-10s %-15.2f %-10s%n",
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getPricePerNight(),
                    room.isAvailable() ? "Yes" : "No");
            }
        }
    }
    
    private static void makeReservation() {
        System.out.println("\n=== Make a Reservation ===");
        System.out.print("Enter room number to book: ");
        String roomNumber = scanner.nextLine();
        
        Room room = roomService.findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("Room not found or not available.");
            return;
        }
        
        LocalDate checkIn = getDateInput("Enter check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = getDateInput("Enter check-out date (YYYY-MM-DD): ");
        
        // Validate dates
        if (checkOut.isBefore(checkIn.plusDays(1))) {
            System.out.println("Error: Minimum stay is 1 night");
            return;
        }
        
        // Check availability
        if (!reservationService.isRoomAvailable(room.getRoomId(), checkIn, checkOut)) {
            System.out.println("Room not available for selected dates");
            return;
        }
        
        // Get guest info
        System.out.print("Enter guest name: ");
        String guestName = scanner.nextLine();
        System.out.print("Enter guest email: ");
        String guestEmail = scanner.nextLine();
        
        // Calculate total price
        double totalPrice = reservationService.calculateTotalCost(room.getRoomId(), checkIn, checkOut);
        
        // Confirm reservation
        System.out.printf("\nReservation Summary:%n");
        System.out.printf("Room: %s (%s)%n", room.getRoomNumber(), room.getRoomType());
        System.out.printf("Dates: %s to %s (%d nights)%n", checkIn, checkOut, 
            ChronoUnit.DAYS.between(checkIn, checkOut));
        System.out.printf("Total Price: $%.2f%n", totalPrice);
        System.out.print("Confirm reservation? (Y/N): ");
        String confirm = scanner.nextLine();
        
        if (confirm.equalsIgnoreCase("Y")) {
            // Make reservation
            Reservation reservation = reservationService.makeReservation(
                room.getRoomId(), guestName, guestEmail, checkIn, checkOut, room.getPricePerNight());
            
            if (reservation != null) {
                System.out.println("Reservation created successfully!");
                System.out.println("Your reservation ID is: " + reservation.getReservationId());
                
                // Process payment
                processPayment(reservation);
            } else {
                System.out.println("Failed to create reservation.");
            }
        } else {
            System.out.println("Reservation cancelled.");
        }
    }
    
    private static void processPayment(Reservation reservation) {
        System.out.println("\n=== Payment Processing ===");
        System.out.printf("Total amount due: $%.2f%n", reservation.getTotalPrice());
        
        System.out.println("Select payment method:");
        System.out.println("1. Credit Card");
        System.out.println("2. Debit Card");
        System.out.println("3. Cash");
        System.out.print("Enter your choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        String paymentMethod = switch (choice) {
            case 1 -> "Credit Card";
            case 2 -> "Debit Card";
            case 3 -> "Cash";
            default -> {
                System.out.println("Invalid choice. Defaulting to Credit Card.");
                yield "Credit Card";
            }
        };
        
        boolean paymentSuccess = paymentService.processPayment(
            reservation.getReservationId(), reservation.getTotalPrice(), paymentMethod);
        
        if (paymentSuccess) {
            Payment payment = paymentService.getPaymentDetails(reservation.getReservationId());
            reservationService.generateReceipt(reservation, payment);
            System.out.println("Payment processed successfully!");
        } else {
            System.out.println("Payment processing failed.");
        }
    }
    
    private static void manageReservation() {
        System.out.println("\n=== Manage Reservation ===");
        System.out.print("Enter your reservation ID: ");
        int reservationId = scanner.nextInt();
        scanner.nextLine();
        
        Reservation reservation = reservationService.getReservationDetails(reservationId);
        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }
        
        System.out.println("\nReservation Details:");
        System.out.println("ID: " + reservationId);
        System.out.println("Guest: " + reservation.getGuestName());
        System.out.println("Email: " + reservation.getGuestEmail());
        System.out.println("Dates: " + reservation.getCheckInDate() + " to " + reservation.getCheckOutDate());
        System.out.printf("Total Price: $%.2f%n", reservation.getTotalPrice());
        System.out.println("Status: " + reservation.getStatus());
        
        Payment payment = paymentService.getPaymentDetails(reservationId);
        if (payment != null) {
            System.out.println("\nPayment Details:");
            System.out.println("Amount: $" + payment.getAmount());
            System.out.println("Method: " + payment.getPaymentMethod());
            System.out.println("Date: " + payment.getPaymentDate());
        }
        
        if (reservation.getStatus().equals("Confirmed")) {
            System.out.print("\nDo you want to cancel this reservation? (Y/N): ");
            String choice = scanner.nextLine();
            
            if (choice.equalsIgnoreCase("Y")) {
                boolean cancelled = reservationService.cancelReservation(reservationId);
                System.out.println(cancelled ? 
                    "Reservation cancelled successfully." : 
                    "Failed to cancel reservation.");
            }
        }
    }
    
    private static LocalDate getDateInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String dateStr = scanner.nextLine();
            
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                if (date.isBefore(LocalDate.now())) {
                    System.out.println("Error: Date cannot be in the past");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
    }
}