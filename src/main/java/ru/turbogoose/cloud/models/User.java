package ru.turbogoose.cloud.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "username", unique = true)
    @Size(min = 2, max = 100, message = "Username length must be in between 2 and 100 symbols")
    private String username;
    @Column(name = "password")
    @Min(value = 5, message = "Password must have at least 5 symbols")
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
