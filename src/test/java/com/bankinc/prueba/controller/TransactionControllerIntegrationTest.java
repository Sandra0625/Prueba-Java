package com.bankinc.prueba.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullTransactionFlow() throws Exception {
        // Generar tarjeta
        MvcResult gen = mockMvc.perform(post("/cards/generate").param("productId", "PROD01"))
                .andExpect(status().isOk())
                .andReturn();
        String cardId = gen.getResponse().getContentAsString().trim();

        // Enrolar y recargar
        mockMvc.perform(post("/cards/{cardId}/enroll", cardId)).andExpect(status().isOk());
        mockMvc.perform(post("/cards/{cardId}/recharge", cardId).param("amount", "50.00")).andExpect(status().isOk());

        // Realizar compra
        Map<String, Object> body = Map.of("cardId", cardId, "price", 12.34);
        String json = objectMapper.writeValueAsString(body);
        MvcResult purchase = mockMvc.perform(post("/transaction/purchase")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        String resp = purchase.getResponse().getContentAsString();
        Map<?,?> map = objectMapper.readValue(resp, Map.class);
        assertThat(map.get("transactionId")).isNotNull();
        String transactionId = map.get("transactionId").toString();

        // Consultar transacción
        MvcResult got = mockMvc.perform(get("/transaction/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andReturn();
        String gotResp = got.getResponse().getContentAsString();
        assertThat(gotResp).contains(transactionId);

        // Anular transacción
        String annulJson = objectMapper.writeValueAsString(Map.of("transactionId", transactionId));
        mockMvc.perform(post("/transaction/anulation").contentType(MediaType.APPLICATION_JSON).content(annulJson))
                .andExpect(status().isOk());
    }
}
