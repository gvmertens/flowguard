package br.com.flowguard.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

public record DeliverySignal(
        @NotBlank @Size(max = 80) String repository,
        @NotBlank @Size(max = 40) String pullRequestId,
        @NotBlank @Size(max = 4_000) String diffSummary,
        @NotEmpty List<@Size(max = 3_000) String> ciLogs,
        List<@Size(max = 120) String> changedModules) implements Serializable {
}
