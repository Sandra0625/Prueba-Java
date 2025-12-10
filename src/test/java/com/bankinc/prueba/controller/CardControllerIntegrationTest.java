package com.bankinc.prueba.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCardFlow() throws Exception {
        // 1) Generar tarjeta
        MvcResult gen = mockMvc.perform(post("/cards/generate").param("productId", "PROD01"))
                .andExpect(status().isOk())
                .andReturn();
        String cardId = gen.getResponse().getContentAsString().trim();
        assertThat(cardId).isNotBlank();

        // 2) Enrolar
        mockMvc.perform(post("/cards/{cardId}/enroll", cardId)).andExpect(status().isOk());

        // 3) Recargar
        mockMvc.perform(post("/cards/{cardId}/recharge", cardId).param("amount", "100.50")).andExpect(status().isOk());

        // 4) Consultar saldo
        MvcResult bal = mockMvc.perform(get("/cards/{cardId}/balance", cardId))
                .andExpect(status().isOk())
                .andReturn();

        String balStr = bal.getResponse().getContentAsString().trim();
        BigDecimal balance = new BigDecimal(balStr);
        assertThat(balance).isEqualByComparingTo(new BigDecimal("100.50"));
    }
}
