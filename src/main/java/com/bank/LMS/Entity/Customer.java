package com.bank.LMS.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers") // Individual @Column(unique=true) handles the constraints effectively
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name max 30 characters")
    @Column(nullable = false, length = 30)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 80, message = "Email max 80 characters")
    @Column(nullable = false, unique = true, length = 80)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be 8-255 characters")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit phone")
    @Column(unique = true, length = 10)
    private String phone;

    @Past(message = "DOB must be in the past")
    private LocalDate dob;

    @Size(max = 255, message = "Address max 255 characters")
    @Column(length = 255)
    private String address;

    @Size(max = 10, message = "City max 80 characters")
    @Column(length = 10)
    private String city;

    @Size(max = 10, message = "State max 80 characters")
    @Column(length = 10)
    private String state;

    @Pattern(regexp = "^[0-9]{4,12}$", message = "Pincode must be 4–12 digits")
    @Column(length = 12)
    private String pincode;

    @Builder.Default // Crucial: This ensures the Builder uses 'true' instead of 'false'
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(length = 6)
    private String gender;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Since 'active' is a primitive boolean, it defaults to false in Java.
        // If you want it to always be true on creation:
        // this.active = true;
    }



}