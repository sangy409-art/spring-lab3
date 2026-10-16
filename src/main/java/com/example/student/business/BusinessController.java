package com.example.student.business;

import com.example.student.model.ProductionBatch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /**
     * Операция 1. Запуск партии со списанием ингредиентов.
     * POST /api/business/start-batch/{batchId}
     */
    @PostMapping("/start-batch/{batchId}")
    public ResponseEntity<?> startBatch(@PathVariable Long batchId) {
        try {
            ProductionBatch batch = businessService.startBatch(batchId);
            return ResponseEntity.ok(batch);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Операция 2. Завершение партии.
     * POST /api/business/complete-batch/{batchId}
     */
    @PostMapping("/complete-batch/{batchId}")
    public ResponseEntity<?> completeBatch(@PathVariable Long batchId) {
        try {
            return ResponseEntity.ok(businessService.completeBatch(batchId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Операция 3. Превратить заявку в партию.
     * POST /api/business/request-to-batch/{requestId}
     */
    @PostMapping("/request-to-batch/{requestId}")
    public ResponseEntity<?> requestToBatch(@PathVariable Long requestId) {
        try {
            return ResponseEntity.ok(businessService.requestToBatch(requestId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Операция 4. Массовое планирование партии.
     * POST /api/business/plan-batch
     * Body: { "recipeId": 1, "quantity": 10 }
     */
    @PostMapping("/plan-batch")
    public ResponseEntity<?> planBatch(@RequestBody Map<String, Object> body) {
        try {
            Long recipeId = Long.valueOf(body.get("recipeId").toString());
            int quantity = Integer.parseInt(body.get("quantity").toString());
            return ResponseEntity.ok(businessService.planBatch(recipeId, quantity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Операция 5. Закрытие партии с обновлением связанных заявок.
     * POST /api/business/close-batch/{batchId}
     */
    @PostMapping("/close-batch/{batchId}")
    public ResponseEntity<?> closeBatch(@PathVariable Long batchId) {
        try {
            return ResponseEntity.ok(businessService.closeBatch(batchId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}