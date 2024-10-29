package eu.sonderfeld.mathias.kubernetesoperator.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Group("eu.sonderfeld.mathias")
@Version("v1")
@ShortNames("wp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Webpage extends CustomResource<WebpageSpec, WebpageStatus> implements Namespaced {
    WebpageSpec spec;
    WebpageStatus status;
}
