package eu.sonderfeld.mathias.kubernetesoperator.reconciler;

import eu.sonderfeld.mathias.kubernetesoperator.model.Webpage;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.Map;

@Slf4j
@Component
@ControllerConfiguration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebpageReconciler implements Reconciler<Webpage>{
    @Override
    public UpdateControl<Webpage> reconcile(Webpage webpage, Context<Webpage> context) throws Exception {


        var cm = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName())
                .withNamespace(webpage.getMetadata().getNamespace())
                .endMetadata()
                .addToData(Map.of("message", webpage.getSpec().getTest()))
                .build();

        var labels = Map.of("app.kubernetes.io/name", "KubernetesOperator", "app.kubernetes.io/instance", webpage.getMetadata().getName());

        var dp = new DeploymentBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName()+ "-nginx")
                .withNamespace(webpage.getMetadata().getNamespace())
                .endMetadata()
                .withSpec(new DeploymentSpecBuilder()
                        .withReplicas(1)
                        .withSelector(new LabelSelectorBuilder()
                                .addToMatchLabels(labels)
                                .build())
                        .withTemplate(new PodTemplateSpecBuilder()
                                .withNewMetadata()
                                .withName(webpage.getMetadata().getName())
                                .withNamespace(webpage.getMetadata().getNamespace())
                                .addToLabels(labels)
                                .endMetadata()
                                .withSpec(new PodSpecBuilder()
                                        .addNewContainer()
                                        .withImage("nginx:latest")
                                        .withImagePullPolicy("IfNotPresent")
                                        .withName(webpage.getMetadata().getName())
                                        .withPorts(new ContainerPortBuilder()
                                                .withName("http")
                                                .withContainerPort(8080)
                                                .build())
                                        .withResources(new ResourceRequirements()
                                                .edit()
                                                .addToRequests(Map.of("cpu", Quantity.parse("150m")))
                                                .addToLimits(Map.of("memory", Quantity.parse("500Mi")))
                                                .build())
                                        .withVolumeMounts(new VolumeMountBuilder()
                                                .withName("cm")
                                                .withMountPath("/usr/share/nginx/html")
                                                .build())
                                        .endContainer()
                                        .withVolumes(new Volume()
                                                .edit()
                                                .withName("cm")
                                                .editConfigMap()
                                                .withName(cm.getMetadata().getName())
                                                .endConfigMap()
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        var svc = new ServiceBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName())
                .withNamespace(webpage.getMetadata().getNamespace())
                .endMetadata()
                .withSpec(new ServiceSpecBuilder()
                        .withType("ClusterIP")
                        .addToPorts(new ServicePortBuilder()
                                .withName("http")
                                .withPort(8080)
                                .build())
                        .addToSelector(labels)
                        .build())
                .build();

        var role = new RoleBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName())
                .withNamespace(webpage.getMetadata().getNamespace())
                .endMetadata()
                .withRules(new PolicyRuleBuilder()
                        .addToApiGroups("")
                        .addToResources("configMaps")
                        .addToVerbs("read")
                        .build())
                .build();


        var sa = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName())
                .withNamespace(webpage.getMetadata().getNamespace())
                .addToLabels(labels)
                .endMetadata()
                .build();


        var rb = new RoleBindingBuilder()
                .withNewMetadata()
                .withName(webpage.getMetadata().getName())
                .withNamespace(webpage.getMetadata().getNamespace())
                .addToLabels(labels)
                .endMetadata()
                .addNewSubject("", "ServiceAccount", sa.getMetadata().getName(), sa.getMetadata().getNamespace())
                .withNewRoleRef("", "Role", role.getMetadata().getName())
                .build();

        var rbac = context.getClient().rbac();

        if(rbac.roles().resource(role).get() == null) {
            rbac.roles().resource(role).create();
        }

        if(rbac.roleBindings().resource(rb).get() == null) {
            rbac.roleBindings().resource(rb).create();
        }

        if(context.getClient().resource(sa).get() == null) {
            context.getClient().resource(sa).create();
        }
        else context.getClient().resource(sa).update();

        if(context.getClient().configMaps().resource(cm).get() == null) {

            context.getClient().configMaps().resource(cm).create();
        }
        else context.getClient().configMaps().resource(cm).update();

        if(context.getClient().resource(svc).get() == null) {
            context.getClient().resource(svc).create();
        }
        else context.getClient().resource(svc).update();

        if(context.getClient().resource(dp).get() == null) {
            context.getClient().resource(dp).create();
        }
        else context.getClient().resource(dp).update();

        return UpdateControl.updateResource(webpage);
    }


    private void registerResource(Context<Webpage> context, HasMetadata resource) {
        if(!context.getClient().resource(resource).isReady()) {
            context.getClient().resource(resource).create();
        }
        else {
            context.getClient().resource(resource).update();
        }
    }
}

