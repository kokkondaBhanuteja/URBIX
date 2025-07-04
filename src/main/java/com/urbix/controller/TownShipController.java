package com.urbix.controller;

import com.urbix.dto.TownshipDTO;
import com.urbix.entity.Township;
import com.urbix.entity.User;
import com.urbix.repository.TownShipRepository;
import com.urbix.service.TownShipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/api/townships")
public class TownShipController {

    private final TownShipRepository townshipRepository;
    private final TownShipService townshipService;

    @Value("${app.name:UrbixApp}")
    private String appName;

    public TownShipController(TownShipRepository townshipRepository, TownShipService townshipService) {
        this.townshipRepository = townshipRepository;
        this.townshipService = townshipService;
    }

    @GetMapping("/add")
    public String showAddTownshipForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        System.out.println("The user type = " + user.getUserType());
        if (user.getUserType().equalsIgnoreCase("SELLER")) {
            model.addAttribute("township", new Township());
            model.addAttribute("appName", appName);
            return "AddTownship";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Login as a SELLER");
            return "redirect:/login";
        }
    }

    @PostMapping("/add-townShip")
    public String addTownship(@ModelAttribute Township township,
                              @RequestParam("imageFile") MultipartFile file,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            // Convert image file to byte array
            township.setImage(file.getBytes());
            User user = (User) session.getAttribute("loggedInUser");
            township.setUser(user);
            // Save the township with the image
            townshipRepository.save(township);

            redirectAttributes.addFlashAttribute("successMessage", "Township added successfully!");
        } catch (IOException | MultipartException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading image.");
        }
        return "redirect:/api/townships/add";
    }

    // View all townships
    @GetMapping("/view-all")
    public String viewAllTownships(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<TownshipDTO> townships;
        if (user.getUserType().equalsIgnoreCase("BUYER")) {
            townships = townshipService.getAllTownships();
        } else {
            townships = townshipService.getTownshipsByUserId(user.getId());
        }

        model.addAttribute("townships", townships);
        model.addAttribute("appName", appName);
        return "ViewAllTownships";
    }

    // Show township details
    @GetMapping("/details/{id}")
    public String showTownshipDetails(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Township township = townshipService.getTownshipById(id);

        // Check if township exists and belongs to the user (unless admin)
        if (township == null ||
                (!user.getUserType().equalsIgnoreCase("BUYER") && !township.getUser().getId().equals(user.getId()))) {
            model.addAttribute("errorMessage", "Township not found or access denied");
            return "redirect:/api/townships/view-all";
        }

        String base64Image = (township.getImage() != null) ?
                Base64.getEncoder().encodeToString(township.getImage()) : "";

        TownshipDTO townshipDTO = new TownshipDTO(
                township.getId(),
                township.getName(),
                township.getState(),
                township.getDescription(),
                base64Image
        );

        model.addAttribute("township", townshipDTO);
        model.addAttribute("appName", appName);
        return "TownShipDetails";
    }

    // Show update township form
    @GetMapping("/update")
    public String showUpdateForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<TownshipDTO> townships;
        if (user.getUserType().equalsIgnoreCase("ADMIN")) {
            townships = townshipService.getAllTownships();
        } else {
            townships = townshipService.getTownshipsByUserId(user.getId());
        }

        model.addAttribute("townships", townships);
        model.addAttribute("appName", appName);
        return "UpdateTownship";
    }

    // Show update form for specific township
    @GetMapping("/update/{id}")
    public String showUpdateFormForTownship(@PathVariable Long id, Model model, HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Township township = townshipService.getTownshipById(id);

        // Check if township exists and belongs to the user (unless admin)
        if (township == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Township not found");
            return "redirect:/api/townships/update";
        }

        if (!user.getUserType().equalsIgnoreCase("ADMIN") && !township.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to update this township");
            return "redirect:/api/townships/update";
        }

        String base64Image = (township.getImage() != null) ?
                Base64.getEncoder().encodeToString(township.getImage()) : "";

        TownshipDTO townshipDTO = new TownshipDTO(
                township.getId(),
                township.getName(),
                township.getState(),
                township.getDescription(),
                base64Image
        );

        model.addAttribute("township", townshipDTO);
        model.addAttribute("appName", appName);
        return "UpdateTownship";
    }

    // Process township update
    @PostMapping("/update/{id}")
    public String updateTownship(@PathVariable Long id,
                                 @ModelAttribute Township township,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Township existingTownship = townshipService.getTownshipById(id);

        // Check if township exists and belongs to the user (unless admin)
        if (existingTownship == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Township not found");
            return "redirect:/api/townships/update";
        }

        if (!user.getUserType().equalsIgnoreCase("SELLER") && !existingTownship.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to update this township");
            return "redirect:/api/townships/update";
        }

        try {
            township.setId(id);
            township.setUser(existingTownship.getUser());
            Township updatedTownship = townshipService.updateTownship(township, imageFile);

            if (updatedTownship != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Township updated successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update township");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing image: " + e.getMessage());
        }

        return "redirect:/api/townships/view-all";
    }

    // Show remove township page
    @GetMapping("/remove")
    public String showRemoveTownshipPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<TownshipDTO> townships;
        if (user.getUserType().equalsIgnoreCase("ADMIN")) {
            townships = townshipService.getAllTownships();
        } else {
            townships = townshipService.getTownshipsByUserId(user.getId());
        }

        model.addAttribute("townships", townships);
        model.addAttribute("appName", appName);
        return "removeTownship";
    }

    // Delete township
    @PostMapping("/delete/{id}")
    public String deleteTownship(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Township township = townshipService.getTownshipById(id);

        // Check if township exists and belongs to the user (unless admin)
        if (township == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Township not found");
            return "redirect:/api/townships/remove";
        }

        if (!user.getUserType().equalsIgnoreCase("ADMIN") && !township.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to delete this township");
            return "redirect:/api/townships/remove";
        }

        try {
            townshipService.deleteTownshipById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Township deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting township: " + e.getMessage());
        }

        return "redirect:/api/townships/remove";
    }
}