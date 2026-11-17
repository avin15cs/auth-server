package io.github.avin15cs.authserver.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSpaController {

    @GetMapping({"/", "/dashboard", "/clients", "/clients/new", "/clients/{id}", "/users", "/invite"})
    public String adminUi() {
        return "forward:/index.html";
    }
}
