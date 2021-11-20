package omar.spring.pps.controlls;

import lombok.extern.slf4j.Slf4j;
import omar.spring.pps.DTO.UserDTO;
import omar.spring.pps.DTO.UserMapper;
import omar.spring.pps.data.entities.Pic;
import omar.spring.pps.data.entities.PicCategory;
import omar.spring.pps.data.entities.PicStatus;
import omar.spring.pps.data.entities.User;
import omar.spring.pps.service.PicService;
import omar.spring.pps.service.SecurityService;
import omar.spring.pps.service.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@Controller
public class MainController {
    private final SecurityService securityService;
    private final PicService picService;
    private final UserMapper userMapper;

    private boolean logout = false;

    @Autowired

    public MainController(SecurityService securityService, PicService picService, UserMapper userMapper) {
        this.securityService = securityService;
        this.picService = picService;
        this.userMapper = userMapper;
    }

    @RequestMapping(value = {"/exit"})
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        logout = true;
        return "redirect:";
    }

    @RequestMapping(value = {"/", "/index", ""})
    public String getMainPage(Model model) {
        log.info("{}:{}", getClass().getSimpleName(), "/index");
        model.addAttribute("hello_message", "Welcome to our website!");
        List<Pic> list = picService.getAllByStatus(List.of(PicStatus.APPROVED));

        SecurityContext securityContext = SecurityContextHolder.getContext();

        if (securityContext.getAuthentication() != null)
            log.info("{}:{}", getClass().getSimpleName(), securityContext.getAuthentication().toString());

        model.addAttribute(new Pic());//case user choose to upload a pic
        model.addAttribute("pics", list);
        model.addAttribute("categories", PicCategory.values());

        if (logout) {
            model.addAttribute("logout", true);
        }
        logout = false;
        return "index";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        if (securityService.isAuthenticated()) {
            return "redirect:/";
        }
        model.addAttribute("user", new UserDTO());
        return "/registration";
    }

    @PostMapping("/registration/new")
    public String registration(@ModelAttribute("user") @Valid UserDTO user, BindingResult bindingResult) {
        if (!user.getPassword().equals(user.getPasswordConfirm())) {
            log.error("Passwords don't match.");
            bindingResult.rejectValue("passwordConfirm", "", "Password Fields Must match.");
        }
        UserDetailsService userService = (UserDetailsService) securityService.userService();
        if (userService.loadUserByUsername(user.getUsername()) != null) {
            log.error("A User with this email already exists.");
            bindingResult.rejectValue("username", "", "A User with this email already exists.");
        }
        if (bindingResult.hasErrors()) {
            return "/registration :: RegistrationFragment";
        }
        userService.saveToDefaults(userMapper.toUser(user));
        return "redirect:/index";
    } @RequestMapping("/modal/{type}")
    public String getModal
            (@PathVariable ModalType type, Model model, @RequestParam(required = false) List<ObjectError> errors) {
        log.info("Now, I'm opening a modal of type {}", type);
        switch (type) {
            case LOGIN:
                model.addAttribute("user", new User());
                model.addAttribute("title", "Login");
                model.addAttribute("info", "Please, enter your credentials.");
                break;
            case INFO:
                model.addAttribute("title", "");
                model.addAttribute("info", "....some information");
                break;
            case DELETE:
                model.addAttribute("title", "Confirm Deleting a pic !");
                model.addAttribute("question", "Are you sure you want to delete this pic?");
                model.addAttribute("info", "....some errors");
                break;
            case ERROR:
                model.addAttribute("title", null);
                model.addAttribute("errors", errors);
                model.addAttribute("info", "....some errors");
                break;

        }
        return "fragments/modal";
    }
    }

