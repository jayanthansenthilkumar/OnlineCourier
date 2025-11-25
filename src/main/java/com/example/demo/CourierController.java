package com.example.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CourierController {

    @GetMapping(value = "/home")
    public String home(Model model) {
        return "home";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @Autowired
    private AdminService adminService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private StaffService staffService;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private CourierDetailRepository courierdetailRepository;
    @Autowired
    private DeliverLogRepository deliverlogRepository;

    @GetMapping("/admin")
    public String adminPage(User user, Model model) {
        model.addAttribute("user", new User());
        return "admin";
    }

    @GetMapping("/customer")
    public String cusPage(User user, Model model) {
        model.addAttribute("user", new User());
        return "customer";
    }

    @GetMapping("/staff")
    public String staffPage(Staffuser user, Model model) {
        model.addAttribute("user", new Staffuser());
        return "staff";
    }

    @GetMapping("/stafflogin")
    public String staffloginPage(Model model) {
        model.addAttribute("user", new Staffuser());
        return "stafflogin";
    }

    @GetMapping("/cuslogin")
    public String loginCusPage(Model model) {
        model.addAttribute("user", new User());
        return "cuslogin";
    }

    @PostMapping(value="/login",consumes = {  MediaType.APPLICATION_FORM_URLENCODED_VALUE })
    public String processLogin(User user, Model model, HttpSession session) {
        boolean isValid = adminService.validateLogin(user.getPassword(), user.getUsername());
        if (isValid) {
            // Create session
            session.setAttribute("user", user.getUsername());
            session.setAttribute("userType", "admin");
            session.setMaxInactiveInterval(1800); // 30 minutes
            
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "admin";
        }
    }

    @PostMapping("/stafflogin")
    public String processStaffLogin(@RequestParam String username,
            @RequestParam String password,
            Model model,
            HttpSession session) {
        boolean isValid = staffService.validateStaffLogin(username, password);
        if (isValid) {
            Staff staff = staffService.findByUsername(username);
            session.setAttribute("user", staff.getName());
            session.setAttribute("username", username);
            session.setAttribute("userType", "staff");
            session.setAttribute("location", staff.getLocation());
            session.setMaxInactiveInterval(1800);
            return "redirect:/stafflogin";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "staff";
        }
    }

    @PostMapping("/registerstaff")
    public String registerStaff(@RequestParam String name,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String location,
            HttpSession session) {
        Staff staff = new Staff();
        staff.setName(name);
        staff.setUsername(username);
        staff.setPassword(password);
        staff.setLocation(location);
        staffService.saveStaff(staff);
        
        session.setAttribute("user", name);
        session.setAttribute("username", username);
        session.setAttribute("userType", "staff");
        session.setAttribute("location", location);
        session.setMaxInactiveInterval(1800);
        return "redirect:/stafflogin";
    }

    @GetMapping("/managestaff")
    public String manageStaff(Staffuser user, Model model) {
        List<Staff> staffList = staffRepository.findAll();
        model.addAttribute("staffList", staffList);
        return "managestaff";
    }

    @PostMapping("/removestaff")
    public String removeStaff(@RequestParam Long id) {
        staffRepository.deleteById(id);
        return "redirect:/managestaff";
    }

    @PostMapping("/cuslogin")
    public String processCusLogin(User user, Model model, HttpSession session) {
        boolean isValid = customerService.validateCusLogin(user.getPassword(), user.getUsername());
        if (isValid) {
            // Create session
            session.setAttribute("user", user.getUsername());
            session.setAttribute("userType", "customer");
            session.setMaxInactiveInterval(1800); // 30 minutes
            
            return "redirect:/cuslogin";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "customer";
        }
    }

    @GetMapping("/sendcourier")
    public String sendCourierPage() {
        return "sendcourier";
    }

    @PostMapping("/submitCourier")
    public ResponseEntity<byte[]> submitCourier(@RequestParam String pickupAddress, @RequestParam String fromName,
            @RequestParam String pickupCity, @RequestParam String paymentMethod,
            @RequestParam String toName, @RequestParam String toMobile, @RequestParam String destinationAddress,
            @RequestParam String destinationCity, @RequestParam double weight, @RequestParam double totalCost) {

        // Generate random courier tracking number
        String trackingNumber = generateTrackingNumber();

        // Generate the courier receipt image
        BufferedImage image = generateReceiptImage(fromName, pickupAddress, pickupCity, paymentMethod, toName, toMobile,
                destinationAddress,
                destinationCity, weight, totalCost, trackingNumber);

        // Convert BufferedImage to byte array
        byte[] imageBytes = convertImageToByteArray(image);

        // Set response headers for image download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "courier_receipt.png");

        CourierDetails courierDetails = new CourierDetails();
        courierDetails.setFromName(fromName);
        courierDetails.setPickupAddress(pickupAddress);
        courierDetails.setPickupCity(pickupCity);
        courierDetails.setToName(toName);
        courierDetails.setToMobile(toMobile);
        courierDetails.setDestinationAddress(destinationAddress);
        courierDetails.setDestinationCity(destinationCity);
        courierDetails.setPaymentMethod(paymentMethod);
        courierDetails.setWeight(weight);
        courierDetails.setTotalCost(totalCost);
        courierDetails.setTrackingNumber(trackingNumber);
        courierDetails.setStatus("Booked");

        installCourier(courierDetails);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
    @PostMapping("/installCourier")
    public ResponseEntity<CourierDetails> installCourier(@RequestBody CourierDetails courierDetails) {
        CourierDetails savedCourier = courierdetailRepository.save(courierDetails);
        return ResponseEntity.ok(savedCourier);
    }

    private String generateTrackingNumber() {
        Random random = new Random();
        int trackingNumber = 100000 + random.nextInt(900000);
        return String.valueOf(trackingNumber);
    }

    private BufferedImage generateReceiptImage(String fromName, String pickupAddress, String pickupCity,
            String paymentMethod, String toName,
            String toMobile, String destinationAddress, String destinationCity, double weight, double totalCost,
            String trackingNumber) {
        int width = 600;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));

        int x = 50;
        int y = 50;
        g2d.drawString("Courier Receipt", x, y);
        y += 30;
        g2d.drawString("From Name: " + fromName, x, y);
        y += 20;
        g2d.drawString("Pickup Address: " + pickupAddress, x, y);
        y += 20;
        g2d.drawString("Pickup City: " + pickupCity, x, y);
        y += 20;
        g2d.drawString("To Name: " + toName, x, y);
        y += 20;
        g2d.drawString("To Mobile: " + toMobile, x, y);
        y += 20;
        g2d.drawString("Destination Address: " + destinationAddress, x, y);
        y += 20;
        g2d.drawString("Destination City: " + destinationCity, x, y);
        y += 20;
        g2d.drawString("Weight of Parcel: " + weight + " kg", x, y);
        y += 20;
        g2d.drawString("Total Cost: Rs" + totalCost, x, y);
        y += 20;
        g2d.drawString("Payment Method: " + paymentMethod, x, y);
        y += 20;
        g2d.drawString("Tracking Number: " + trackingNumber, x, y);
        y += 20;
        g2d.drawString("Status: " + "Booked", x, y);
        
        g2d.dispose();

        return image;
    }

    private byte[] convertImageToByteArray(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    @GetMapping("/pickupLog")
    public String pickupLog(Model model) {
        List<CourierDetails> courierDetailsList = courierdetailRepository.findAll();
        model.addAttribute("courierDetailsList", courierDetailsList);
        return "pickupLog";
    }

    @GetMapping("/enterTrackingNumber")
    public String enterTrackingNumberPage() {
        return "enterTrackingNumber";
    }

    @GetMapping("/enterfullname")
    public String enterfullnamePage() {
        return "enterfullname";
    }

    @GetMapping("/deliverLog")
    public String deliverlogPage(Model model) {
        List<DeliverLog> deliverLogs = deliverlogRepository.findAll();
        model.addAttribute("deliverLogs", deliverLogs);
        return "deliverLog";
    }

    @GetMapping("/checkStatus")
    public String checkStatus(@RequestParam String trackingNumber, Model model) {
        Optional<CourierDetails> optionalCourier = courierdetailRepository.findByTrackingNumber(trackingNumber);

        if (optionalCourier.isPresent()) {
            CourierDetails courier = optionalCourier.get();
            String status = courier.getStatus();
            model.addAttribute("status", status);
        } else {
            model.addAttribute("status", "Not Found");
        }
        return "displayStatus";
    }

    @PostMapping("/pickup")
    public String pickup(@RequestParam String trackingNumber) {
        CourierDetails courierDetails = courierdetailRepository.findByTrackingNumber(trackingNumber).orElse(null);
        if (courierDetails != null) {
            courierDetails.setStatus("Picked");
            courierdetailRepository.save(courierDetails);
            deliverlogRepository.save(new DeliverLog(courierDetails.getTrackingNumber(),
                    courierDetails.getToName(), courierDetails.getDestinationAddress(),
                    courierDetails.getDestinationCity()));
            return "pickupsuccess";
        }
        return "Courier not found or already picked.";
    }

    @PostMapping("/deliver")
    public String deliver(@RequestParam String trackingNumber) {
        CourierDetails courierDetails = courierdetailRepository.findByTrackingNumber(trackingNumber).orElse(null);
        if (courierDetails != null) {
            courierDetails.setStatus("Delivered");
            courierdetailRepository.save(courierDetails);
            return "deliversuccess";
        }
        return "Courier not found or already picked.";
    }

    @GetMapping("/searchByName")
    public String searchByName(@RequestParam String fromName, Model model) {
        List<CourierDetails> courierDetailsList = courierdetailRepository.findByFromName(fromName);
        model.addAttribute("courierDetailsList", courierDetailsList);
        return "displayResults";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/home";
    }
    
    @GetMapping("/session-info")
    public String getSessionInfo(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            model.addAttribute("username", session.getAttribute("user"));
            model.addAttribute("userType", session.getAttribute("userType"));
            model.addAttribute("sessionId", session.getId());
            model.addAttribute("creationTime", new java.util.Date(session.getCreationTime()));
            model.addAttribute("lastAccessTime", new java.util.Date(session.getLastAccessedTime()));
            model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
        }
        return "sessionInfo";
    }
    
    @GetMapping("/statistics")
    public String getStatistics(Model model, HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        
        if ("admin".equals(userType)) {
            long totalCouriers = courierdetailRepository.count();
            long bookedCouriers = courierdetailRepository.findByStatus("Booked").size();
            long pickedCouriers = courierdetailRepository.findByStatus("Picked").size();
            long deliveredCouriers = courierdetailRepository.findByStatus("Delivered").size();
            long totalStaff = staffRepository.count();
            
            model.addAttribute("totalCouriers", totalCouriers);
            model.addAttribute("bookedCouriers", bookedCouriers);
            model.addAttribute("pickedCouriers", pickedCouriers);
            model.addAttribute("deliveredCouriers", deliveredCouriers);
            model.addAttribute("totalStaff", totalStaff);
            
            return "adminStatistics";
        } else if ("customer".equals(userType)) {
            String username = (String) session.getAttribute("user");
            List<CourierDetails> userCouriers = courierdetailRepository.findByFromName(username);
            
            long totalBookings = userCouriers.size();
            long delivered = userCouriers.stream().filter(c -> "Delivered".equals(c.getStatus())).count();
            long inTransit = userCouriers.stream().filter(c -> "Picked".equals(c.getStatus())).count();
            long pending = userCouriers.stream().filter(c -> "Booked".equals(c.getStatus())).count();
            
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("delivered", delivered);
            model.addAttribute("inTransit", inTransit);
            model.addAttribute("pending", pending);
            
            return "customerStatistics";
        } else if ("staff".equals(userType)) {
            String location = (String) session.getAttribute("location");
            List<CourierDetails> locationCouriers = courierdetailRepository.findByPickupCity(location);
            
            long totalAssigned = locationCouriers.size();
            long pendingPickup = locationCouriers.stream().filter(c -> "Booked".equals(c.getStatus())).count();
            long pendingDelivery = deliverlogRepository.count();
            
            model.addAttribute("totalAssigned", totalAssigned);
            model.addAttribute("pendingPickup", pendingPickup);
            model.addAttribute("pendingDelivery", pendingDelivery);
            model.addAttribute("location", location);
            
            return "staffStatistics";
        }
        
        return "redirect:/home";
    }
    
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        String userType = (String) session.getAttribute("userType");
        
        model.addAttribute("username", username);
        model.addAttribute("userType", userType);
        
        if ("staff".equals(userType)) {
            model.addAttribute("location", session.getAttribute("location"));
        }
        
        return "profile";
    }
    
    @GetMapping("/recent-activity")
    public String getRecentActivity(HttpSession session, Model model) {
        String username = (String) session.getAttribute("user");
        String userType = (String) session.getAttribute("userType");
        
        if ("customer".equals(userType)) {
            List<CourierDetails> recentCouriers = courierdetailRepository.findByFromName(username);
            if (recentCouriers.size() > 5) {
                recentCouriers = recentCouriers.subList(0, 5);
            }
            model.addAttribute("recentActivity", recentCouriers);
        } else if ("admin".equals(userType)) {
            List<CourierDetails> allCouriers = courierdetailRepository.findAll();
            if (allCouriers.size() > 10) {
                allCouriers = allCouriers.subList(0, 10);
            }
            model.addAttribute("recentActivity", allCouriers);
        }
        
        return "recentActivity";
    }
}
