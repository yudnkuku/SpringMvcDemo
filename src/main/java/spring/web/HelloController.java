package spring.web;


import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import spring.bean.Person;
import spring.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Date;

@Controller
@RequestMapping("/demo")
@SessionAttributes("sessionAttributes")
public class HelloController {

//    @RequestMapping("hello")
//    @ResponseBody
//    public String hello() {
//        return "Hello from controller";
//    }

    @RequestMapping("/index")
    @ResponseBody
    public String index() {
        return "hello";
    }

    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public String user(ModelMap modelMap) {
        modelMap.put("user", new User());
        return "login";
    }

    @PostMapping("/user")
    public ModelAndView postUser(@ModelAttribute("user") @Valid User user, BindingResult bindingResult) {
        ModelAndView modelAndView = new ModelAndView();
        if (bindingResult.hasErrors()) {
            modelAndView.setViewName("login");
        } else {
            modelAndView.addObject("user",user);
            modelAndView.setViewName("success");
        }
        return modelAndView;
    }

    @RequestMapping("test/{user}")
    public String test(@PathVariable("user") String user, Model model) {
        model.addAttribute("user",user);
        model.addAttribute("sessionAttributes","会话模型属性");
        model.addAttribute("localAttribute","test局部模型属性");
        return "test";
    }

    @RequestMapping("test2")
    public String test2(@SessionAttribute("sessionAttributes") String sessionAttribute) {
        System.out.println("session attribute:" + sessionAttribute);
        return "test";
    }

    @RequestMapping("testMedia")
    public String testMedia(Model model) {
        User user = new User("yuding","yuding","yudnkuku@163.com",new Date());
        model.addAttribute("user",user);
        return "media";
    }

    @RequestMapping("path/{name}")
    @ResponseBody
    public String testName(@PathVariable("name")String name,
                           HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();
        return "contextPath=\"" + contextPath + "\"\nservletPath=" + servletPath +
                "\npathInfo=\"" + pathInfo + "\"\nqueryString=\"" + queryStr + "\"";
    }

    @PostMapping("/person")
    @ResponseBody
    public String testPerson(Person person) {
        return person.toString();
    }

    @GetMapping("/collection")
    @ResponseBody
    public String collection(@RequestParam Collection<Integer> values) {
        return "Converted collection " + values;
    }

    @PostMapping(path = "/multipart")
    @ResponseBody
    public String mulitpart(@RequestPart("name") String name,
                            @RequestPart("file") MultipartFile file) {
        System.out.println(file.getOriginalFilename());
        return name;
    }

}
