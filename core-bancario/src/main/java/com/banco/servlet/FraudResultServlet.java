package com.banco.servlet;

import com.banco.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@WebServlet("/servlet/fraud-result")
public class FraudResultServlet extends HttpServlet {

    @Autowired
    private TransferService transferService;

    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        String body = req.getReader().lines()
                         .collect(Collectors.joining());

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(body, Map.class);

            String txIdStr = (String) data.get("txId");
            Boolean flagged = (Boolean) data.get("flagged");

            if (txIdStr != null && flagged != null && flagged) {
                transferService.updateStatus(UUID.fromString(txIdStr), "FLAGGED");
            }

            System.out.println("[Servlet] Resultado de fraude recibido: " + body);
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"status\":\"updated\"}");
        } catch (Exception e) {
            System.err.println("[Servlet Error] Error en FraudResultServlet: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"error\":\"Invalid request format: " + e.getMessage() + "\"}");
        }
    }
}
