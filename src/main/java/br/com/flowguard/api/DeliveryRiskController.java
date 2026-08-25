package br.com.flowguard.api;

import br.com.flowguard.application.DeliveryRiskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-risk")
public class DeliveryRiskController {
    private final DeliveryRiskService service;

    public DeliveryRiskController(DeliveryRiskService service) {
        this.service = service;
    }

    @PostMapping("/assess")
    @ResponseStatus(HttpStatus.OK)
    public DeliveryAssessment assess(@Valid @RequestBody DeliverySignal signal) {
        return service.assess(signal);
    }
}

