package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public boolean validateCusLogin(String password, String username) {
        username = username.trim();
        password = password.trim();
        Customer customer = customerRepository.findById(username).orElse(null);
        return customer != null && customer.getPassword().equals(password);
    }
}
