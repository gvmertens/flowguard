package br.com.flowguard.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "flowguard.ai.enabled=false",
        "flowguard.github.token=",
        "flowguard.low-code.webhook-url="
})
@AutoConfigureMockMvc
class FrontendResourceTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesFrontendAtRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FlowGuard")))
                .andExpect(content().string(containsString("Avaliar risco")))
                .andExpect(content().string(containsString("/api/v1/delivery-risk/assess")));
    }
}
