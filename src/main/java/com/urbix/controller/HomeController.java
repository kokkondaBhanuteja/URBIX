package com.urbix.controller;

import com.urbix.dto.TownshipDTO;

import com.urbix.dto.UserDTO;
import com.urbix.service.TownShipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.urbix.controller.AuthController.SUCCESS_MESSAGE;

@Controller
public class HomeController {
    @Autowired
    private TownShipService townShipService;
    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(value = "city", required = false, defaultValue = "hyderabad") String city,
                                Model model) {
        Map<String, Object> data = townShipService.processData(city);
        model.addAttribute("locations", data.get("locations"));
        model.addAttribute("prices", data.get("prices"));
        model.addAttribute("areas", data.get("areas"));
        model.addAttribute("selectedCity", city);
        model.addAttribute("availableCities", townShipService.getAvailableCities()); // Add all city options
        return "dashboard";
    }
    @GetMapping("/")
    public String home(Model model) {
        List<TownshipDTO> townships =townShipService.getAllTownships();

        // Group townships by state
        Map<String, List<TownshipDTO>> groupedTownships = townships.stream()
                .collect(Collectors.groupingBy(TownshipDTO::getState));

        model.addAttribute("states", groupedTownships);
        model.addAttribute("pageTitle", "URBIX - Land Investment Platform");
        model.addAttribute("appName", "URBIX");
        model.addAttribute("heroTitle", "Your Gateway to Land Investments Made Simple");
        model.addAttribute("heroSubtitle", "Connect with landowners and discover prime properties through our interactive platform");
        return "home";
    }
    @GetMapping("/login")
    public String login(Model model) {
        UserDTO userDTO = new UserDTO();
        model.addAttribute("userDTO",userDTO);
        return "login";
    }
    @GetMapping("/register")
    public String showRegistrationPage(Model model){
        UserDTO userDTO = new UserDTO();
        model.addAttribute("userDTO",userDTO);
        return "register";
    }
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, "You have been successfully logged out");
        return "redirect:/login?logout";
    }
}