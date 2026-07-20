package com.portfolio.saga.application;

import com.portfolio.saga.domain.OrderSaga;
import com.portfolio.saga.domain.Saga;
import com.portfolio.saga.domain.SagaOrchestrator;
import com.portfolio.saga.domain.SagaStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/saga")
public class SagaController {
    private final InMemorySagaLog sagaLog = new InMemorySagaLog();
    private final SagaOrchestrator orchestrator = new SagaOrchestrator();

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        String orderId = body.getOrDefault("orderId", "order-" + System.currentTimeMillis()).toString();
        boolean failInventory = Boolean.parseBoolean(body.getOrDefault("failInventory", "false").toString());
        boolean failPayment = Boolean.parseBoolean(body.getOrDefault("failPayment", "false").toString());
        boolean failShip = Boolean.parseBoolean(body.getOrDefault("failShip", "false").toString());

        Saga saga = new OrderSaga(sagaLog, orderId, failInventory, failPayment, failShip);
        SagaStatus result = orchestrator.execute(saga);

        return ResponseEntity.ok(Map.of(
                "sagaId", saga.getId(),
                "orderId", orderId,
                "status", result.name()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
