package com.example.auction.raw;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final AuctionService service;
    public PageController(AuctionService service) { this.service = service; }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("state", service.snapshot());
        return "index";
    }
}
