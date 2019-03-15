package spring.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
public class HelloControllerTest extends AbstractControllerTest {

    private MockMvc mockMvc;

    @Before
    public void setup() {
//        this.mockMvc = webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
        this.mockMvc = webAppContextSetup(this.wac).build();
    }


    @Test
    public void index() throws Exception {
        this.mockMvc.perform(get("/demo/index"))
                .andExpect(content().string("hello"));
    }

    //测试控制器返回List类型时返回的模型和视图
    @Test
    public void testList() throws Exception {
        this.mockMvc.perform(get("/demo/list"))
                .andExpect(view().name("demo/list"));
    }

    @Test
    public void testRedirect() throws Exception {
        this.mockMvc.perform(get("/demo/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/demo/redirect/Deacon?age=26"))
                .andExpect(model().attributeDoesNotExist("user"));
    }
}
