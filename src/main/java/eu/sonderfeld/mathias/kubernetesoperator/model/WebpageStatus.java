package eu.sonderfeld.mathias.kubernetesoperator.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebpageStatus {
    boolean enabled;
}
