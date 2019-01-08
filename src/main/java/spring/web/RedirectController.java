package spring.web;


import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("redirect")
public class RedirectController {

    @RequestMapping(value = "/test1", method = RequestMethod.GET)
    public String test1(HttpServletResponse response, ModelMap modelMap) throws IOException {
        modelMap.addAttribute("foo", "bar");
        return "redirect:/redirect/test/{foo}";
    }

    @RequestMapping(value = "/test2", method = RequestMethod.GET)
    public String test2(HttpServletResponse response) throws IOException {
        return "redirect:redirect/test";
    }

    @RequestMapping(value = "/test3", method = RequestMethod.GET)
    public String test3(HttpServletResponse response) throws IOException {
        return "redirect://redirect/test";
    }

    @RequestMapping("test/{foo}")
    @ResponseBody
    public String test2(@PathVariable("foo") String foo) {
        return "hello " + foo;
    }
}