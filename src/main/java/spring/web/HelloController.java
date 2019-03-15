package spring.web;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import spring.bean.Person;
import spring.entity.MyUser;
import spring.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.xml.transform.Result;
import java.util.*;

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

    //构造一个返回List的构造器
    @RequestMapping("list")
    public List<String> testList(Map model) {
        List<String> result = new ArrayList<>();
        result.add("yuding");
        return result;
    }

    @RequestMapping(value = "/register")
    public String testRegister(Model model) {
        model.addAttribute("userName", "Deacon");
        model.addAttribute("age",  26);
        User user = new User();
        user.setUsername("deacon");
        model.addAttribute("user", user);
        return "redirect:/demo/redirect/{userName}";
    }

    @ResponseBody
    @RequestMapping("/redirect/{userName}")
    public String testRedirect(@RequestParam("age") int age,
                               @PathVariable("userName")String userName,
                               Model model) {
        System.out.println("UserName : " + userName + " Age : " + age);
        return "end";
    }

//    @RequestMapping(value = "/login", method = RequestMethod.POST, consumes = "application/json")
//    @ResponseBody
//    public String login(@RequestParam("username") String username,
//                        @RequestParam("password") String password) {
//        return "login success!!!Welcome " + username;
//    }

    @RequestMapping(value = "/login", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public String login2(@RequestBody MyUser user) {
        return "success";
    }
}
