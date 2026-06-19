package com.hmdp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaticImageControllerTest {

    @Test
    void imagePathReturnsSvgPlaceholderWhenLocalFileIsMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StaticImageController()).build();

        mockMvc.perform(get("/imgs/shop/demo-coffee.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("演示图片")));
    }

    @Test
    void typeIconPathReturnsSvgPlaceholderWhenLocalFileIsMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StaticImageController()).build();

        mockMvc.perform(get("/types/ms.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("分类图标")));
    }
}
