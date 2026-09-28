CREATE DATABASE IF NOT EXISTS doctor_db;
CREATE DATABASE IF NOT EXISTS appointment_db;

USE doctor_db;

CREATE TABLE IF NOT EXISTS doctors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    consultation_fee DOUBLE NOT NULL,
    available_slots INT NOT NULL
);

INSERT INTO doctors (id, doctor_name, specialization, consultation_fee, available_slots) VALUES
(1, 'Dr. Nguyen Van A', 'Cardiology', 500000.0, 10),
(2, 'Dr. Tran Thi B', 'Dermatology', 350000.0, 5)
ON DUPLICATE KEY UPDATE 
doctor_name=VALUES(doctor_name), 
specialization=VALUES(specialization), 
consultation_fee=VALUES(consultation_fee), 
available_slots=VALUES(available_slots);
