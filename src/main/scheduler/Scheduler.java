package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.model.Appointment;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.UUID;
import java.util.concurrent.Callable;

public class  Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // search_caregiver_schedule <date>
        // check 1: check if the current logged-in user is either a caregiver or a patient
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login in first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        Vaccine vaccine = null;
        try {
            Date d = Date.valueOf(date);
            PreparedStatement selectDate = con.prepareStatement("SELECT Username FROM Availabilities WHERE Time = ?");
            PreparedStatement getAllVaccines = con.prepareStatement("SELECT * FROM Vaccines");
            selectDate.setDate(1, d);
            ResultSet resultSet1 = selectDate.executeQuery();
            ResultSet resultSet2 = getAllVaccines.executeQuery();
            if (!resultSet1.isBeforeFirst()) {
                // handle empty set
                System.out.println("There's no caregiver available this date! Please change another date!");
            }
            while (resultSet2.next()) {
                vaccine = new Vaccine.VaccineGetter(resultSet2.getString("Name")).get();
                System.out.println(vaccine);
            }
            while (resultSet1.next()) {
                System.out.println("Caregiver: " + resultSet1.getString("Username"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching available caregivers");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // reserve <date> <vaccine>
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: check if the current logged-in user is a patient
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        // check 3: whether there's available dose for this vaccine
        String vaccinename = tokens[2];
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccinename).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when making a reservation");
            e.printStackTrace();
        }
        if (vaccine == null || vaccine.getAvailableDoses() <= 0) {
            System.out.println("There's no available dose for this vaccine! Try another vaccine!");
            return;
        }
        // check 4: whether this patient has already made a reservation on this date
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            PreparedStatement selectAppointment = con.prepareStatement("SELECT * FROM Appointments WHERE Patient = ? AND Appoint_date = ?");
            selectAppointment.setString(1, currentPatient.getUsername());
            selectAppointment.setDate(2, d);
            ResultSet rs1 = selectAppointment.executeQuery();
            if (rs1.isBeforeFirst()) {
                System.out.println("You have already made a reservation on this date!");
                return;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when making a reservation");
            e.printStackTrace();
        }
        Appointment appointment = null;
        try {
            Date d = Date.valueOf(date);
            PreparedStatement assignedCaregiver = con.prepareStatement("SELECT Username FROM Availabilities WHERE Time = ? ORDER BY NEWID()");
            assignedCaregiver.setDate(1, d);
            ResultSet rs2 = assignedCaregiver.executeQuery();
            // check 5: whether there's available caregiver on this date
            if (!rs2.isBeforeFirst()) {
                System.out.println("There's no caregiver available this date! Please change another date!");
                return;
            }
            rs2.next();
            String caregivername = rs2.getString("Username");
            String patientname = currentPatient.getUsername();
            UUID uuid = UUID.randomUUID();
            String appointment_id = uuid.toString();
            appointment = new Appointment.AppointmentBuilder(patientname, caregivername, vaccinename, appointment_id, d).build();
            appointment.saveToDB();
            vaccine.decreaseAvailableDoses(1);
            PreparedStatement deleteAvailability = con.prepareStatement("DELETE FROM Availabilities WHERE Time = ? AND Username = ?");
            deleteAvailability.setDate(1, d);
            deleteAvailability.setString(2, caregivername);
            deleteAvailability.executeUpdate();
            System.out.println(" *** You made a reservation successfully *** ");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when making a reservation");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // check 3: whether this caregiver has already uploaded an availability on this date
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            PreparedStatement selectAvailability = con.prepareStatement("SELECT * FROM Availabilities WHERE Time = ? AND Username = ?");
            selectAvailability.setDate(1, d);
            selectAvailability.setString(2, currentCaregiver.getUsername());
            ResultSet rs1 = selectAvailability.executeQuery();
            if (rs1.isBeforeFirst()) {
                System.out.println("You have already uploaded availability on this date!");
                return;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when making a reservation");
            e.printStackTrace();
        }
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // cancel <appointment_id>
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        // check 1: check if the current logged-in user is either a caregiver or a patient
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login in first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String appointment_id = tokens[1];
        Appointment appointment = null;
        Vaccine vaccine = null;
        try {
            appointment = new Appointment.AppointmentGetter(appointment_id).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when cancelling a appointment");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that this appointment doesn't exist
        if (appointment == null) {
            System.out.println("This appointment doesn't exist!");
            return;
        } else {
            try {
                PreparedStatement selectAppointment = con.prepareStatement("SELECT * FROM Appointments WHERE Appointment_ID = ?");
                selectAppointment.setString(1, appointment_id);
                ResultSet rs1 = selectAppointment.executeQuery();
                if (rs1.next()) {
                    String vaccinename = rs1.getString("Vaccine");
                    Date date = rs1.getDate("Appoint_date");
                    String caregivername = rs1.getString("Caregiver");
                    String patientname = rs1.getString("Patient");
                    // check 4: if the logged user is neither the patient nor the caregiver who made the appointment before
                    // he/she doesn't have the permission to cancel it
                    if (currentPatient != null) {
                        if (!patientname.equals(currentPatient.getUsername())){
                            System.out.println("You are not permitted to cancel this appointment!");
                            return;
                        }
                    }
                    if (currentCaregiver != null) {
                        if (!caregivername.equals(currentCaregiver.getUsername())){
                            System.out.println("You are not permitted to cancel this appointment!");
                            return;
                        }
                    }
                    vaccine = new Vaccine.VaccineGetter(vaccinename).get();
                    vaccine.increaseAvailableDoses(1);
                    PreparedStatement addAvailability = con.prepareStatement("INSERT INTO Availabilities VALUES (? , ?)");
                    addAvailability.setDate(1, date);
                    addAvailability.setString(2, caregivername);
                    addAvailability.executeUpdate();
                    PreparedStatement deleteAppointment = con.prepareStatement("DELETE FROM Appointments WHERE Appointment_ID = ?");
                    deleteAppointment.setString(1, appointment_id);
                    deleteAppointment.executeUpdate();
                    System.out.println(" *** You cancelled a reservation successfully *** ");
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when cancelling a appointment");
                e.printStackTrace();
            }
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // show_appointments
        // check 1: check if the current logged-in user is either a caregiver or a patient
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login in first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if (currentCaregiver != null) {
            try {
                PreparedStatement selectCaregiver = con.prepareStatement("SELECT * FROM Appointments WHERE Caregiver = ?");
                selectCaregiver.setString(1, currentCaregiver.getUsername());
                ResultSet rs1 = selectCaregiver.executeQuery();
                if (!rs1.isBeforeFirst()) {
                    // handle empty set
                    System.out.println("You have no reservation!");
                }
                while (rs1.next()) {
                    System.out.println("{Appointment_ID}: " + rs1.getString("Appointment_ID") +
                            "  {Vaccine_name}: " + rs1.getString("Vaccine") +
                            "  {Date}: " + rs1.getDate("Appoint_date") +
                            "  {Patient}: " + rs1.getString("Patient"));
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when showing appointments");
                e.printStackTrace();
            }
        }
        if (currentPatient != null) {
            try {
                PreparedStatement selectPatient = con.prepareStatement("SELECT * FROM Appointments WHERE Patient = ?");
                selectPatient.setString(1, currentPatient.getUsername());
                ResultSet rs2 = selectPatient.executeQuery();
                if (!rs2.isBeforeFirst()) {
                    // handle empty set
                    System.out.println("You have no reservation!");
                }
                while (rs2.next()) {
                    System.out.println("{Appointment_ID}: " + rs2.getString("Appointment_ID") +
                            "  {Vaccine_name}: " + rs2.getString("Vaccine") +
                            "  {Date}: " + rs2.getDate("Appoint_date") +
                            "  {Caregiver}: " + rs2.getString("Caregiver"));
                }
            } catch (SQLException e) {
                System.out.println("Error occurred when showing appointments");
                e.printStackTrace();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // logout
        // check 1: if no one is logged-in now, no need to log out
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Not logged in yet!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        if (currentCaregiver != null) {
            System.out.println("Caregiver " + currentCaregiver.getUsername() + " logged out");
            currentCaregiver = null;
        }
        if (currentPatient != null) {
            System.out.println("Patient " + currentPatient.getUsername() + " logged out");
            currentPatient = null;
        }
    }
}
