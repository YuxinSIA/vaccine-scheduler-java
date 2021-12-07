package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;

public class Appointment {
    private String patient;
    private String caregiver;
    private String vaccine;
    private final String appointment_id;
    private Date appoint_date;

    private Appointment(Appointment.AppointmentBuilder builder) {
        this.patient = builder.patient;
        this.caregiver = builder.caregiver;
        this.vaccine = builder.vaccine;
        this.appointment_id = builder.appointment_id;
        this.appoint_date = builder.appoint_date;
    }

    private Appointment(Appointment.AppointmentGetter getter) {
        this.patient = getter.patient;
        this.caregiver = getter.caregiver;
        this.vaccine = getter.vaccine;
        this.appointment_id = getter.appointment_id;
        this.appoint_date = getter.appoint_date;
    }

    // Getters
    public String getPatient() {
        return patient;
    }

    public String getCaregiver() {
        return caregiver;
    }

    public String getVaccine() {
        return vaccine;
    }

    public String getAppointment_id() {
        return appointment_id;
    }

    public Date getAppoint_date() {
        return appoint_date;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setString(1, this.patient);
            statement.setString(2, this.caregiver);
            statement.setString(3, this.vaccine);
            statement.setString(4, this.appointment_id);
            statement.setDate(5, this.appoint_date);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private String patient;
        private String caregiver;
        private String vaccine;
        private final String appointment_id;
        private Date appoint_date;

        public AppointmentBuilder(String patient, String caregiver, String vaccine, String appointment_id, Date appoint_date) {
            this.patient = patient;
            this.caregiver = caregiver;
            this.vaccine = vaccine;
            this.appointment_id = appointment_id;
            this.appoint_date = appoint_date;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public static class AppointmentGetter {
        private String patient;
        private String caregiver;
        private String vaccine;
        private final String appointment_id;
        private Date appoint_date;

        public AppointmentGetter(String appointment_id) {
            this.appointment_id = appointment_id;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAppointment = "SELECT * FROM Appointments WHERE Appointment_ID = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                statement.setString(1, this.appointment_id);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String patient = resultSet.getString("Patient");
                    String caregiver = resultSet.getString("Caregiver");
                    String vaccine = resultSet.getString("Vaccine");
                    Date appoint_date = resultSet.getDate("Appoint_date");
                    return new Appointment(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
