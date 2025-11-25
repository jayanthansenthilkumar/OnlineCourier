package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepository;

    public void saveStaff(Staff staff) {
        staffRepository.save(staff);
    }

    public boolean validateStaffLogin(String username, String password) {
        username = username.trim();
        password = password.trim();
        Staff staff = staffRepository.findByUsername(username);
        return staff != null && staff.getPassword().equals(password);
    }

    public Staff findByUsername(String username) {
        return staffRepository.findByUsername(username);
    }
}
