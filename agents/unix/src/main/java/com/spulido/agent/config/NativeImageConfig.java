package com.spulido.agent.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import com.spulido.agent.worker.http.dto.ExploitationKnowledgeRequest;
import com.spulido.agent.worker.http.dto.ExploitationKnowledgeResponse;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;
import com.spulido.agent.worker.http.dto.PlanResponse;
import com.spulido.agent.worker.http.dto.PlanStepResponse;
import com.spulido.agent.worker.http.dto.RegisterReplicatedRequest;
import com.spulido.agent.worker.http.dto.RegisterReplicatedResponse;
import com.spulido.agent.worker.http.dto.RemediationReportRequest;
import com.spulido.agent.worker.http.dto.RemediationReportResponse;
import com.spulido.agent.worker.http.dto.RemediationStrategyRequest;
import com.spulido.agent.worker.http.dto.RemediationStrategyResponse;
import com.spulido.agent.worker.http.dto.ReplicationRequestBody;
import com.spulido.agent.worker.http.dto.ReplicationRequestResponse;
import com.spulido.agent.worker.http.dto.ReplicationStatusResponse;
import com.spulido.agent.worker.http.dto.StepStatusResponse;
import com.spulido.agent.worker.http.dto.StepStatusUpdate;
import com.spulido.agent.worker.http.dto.TeardownReportRequest;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupRequest;
import com.spulido.agent.worker.http.dto.VulnerabilityLookupResponse;

@Configuration
@ImportRuntimeHints(NativeImageResourceHints.class)
public class NativeImageConfig {
}

class NativeImageResourceHints implements RuntimeHintsRegistrar {

    /**
     * DTOs exchanged with the central platform over HTTP. In a GraalVM native
     * image Jackson cannot introspect these reflectively unless they are
     * registered for binding, which is why heartbeat/plan calls failed with
     * "Type definition error" until they were added here.
     * {@link BindingReflectionHintsRegistrar} follows nested/reachable property
     * types (e.g. {@code CveEntry}, {@code PlanStepResponse}) transitively.
     */
    private static final Class<?>[] JSON_BINDING_TYPES = {
            ExploitationKnowledgeRequest.class,
            ExploitationKnowledgeResponse.class,
            HeartbeatResponse.class,
            PlanResponse.class,
            PlanStepResponse.class,
            RegisterReplicatedRequest.class,
            RegisterReplicatedResponse.class,
            RemediationReportRequest.class,
            RemediationReportResponse.class,
            RemediationStrategyRequest.class,
            RemediationStrategyResponse.class,
            ReplicationRequestBody.class,
            ReplicationRequestResponse.class,
            ReplicationStatusResponse.class,
            StepStatusResponse.class,
            StepStatusUpdate.class,
            TeardownReportRequest.class,
            VulnerabilityLookupRequest.class,
            VulnerabilityLookupResponse.class
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("scripts/*");
        hints.resources().registerPattern("tools/**");

        BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
        for (Class<?> type : JSON_BINDING_TYPES) {
            bindingRegistrar.registerReflectionHints(hints.reflection(), type);
        }
    }
}
