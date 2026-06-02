package com.banco.servlet;

import com.banco.model.Transaction;
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
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/transfer")
public class TransferServlet extends HttpServlet {

    @Autowired
    private TransferService service;

    @Override
    public void init() throws ServletException {
        super.init();
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws IOException {

        System.out.println("[Servlet] ---> ¡Petición de transferencia capturada por TransferServlet! <---");

        String body = req.getReader().lines()
                         .collect(Collectors.joining());

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = mapper.readValue(body, Map.class);
        
        String toUser = (String) data.get("toUser");
        BigDecimal amount = new BigDecimal(data.get("amount").toString());

        String fromUser = (String) req.getAttribute("username");

        if (fromUser == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"error\":\"Unauthorized: Missing user in token\"}");
            return;
        }

        Transaction tx = service.processTransfer(fromUser, toUser, amount);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"status\":\"ok\",\"txId\":\"" + tx.getId() + "\",\"amount\":" + tx.getAmount() + "}");
    }
}
