package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;// getter anootation 
import lombok.Setter;//setter annotation
// Getter & setter annotation when used at class level, applies to all attributes of the class
//If you apply them at the field level, it only generates getters and setters for that specific field

@Data // generates getters & setters for all properties in the class defined(so need
      // not deine them explicitly)
@Table(name = "admin") // change table name of the below defined class (Admin) to (admin)
@Entity // maps below class(Admin) to (Admin) table in db (if it exists in db)
// else @Table is used to map below defined class to table name specified in
// @Table in the db
// @Column: used to change column name

public class Admin {

    @Id // Username is the primary key
    private String username;
    private String password;

    // Explicit getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}