package com.spulido.tfg.domain.remediation.db;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.spulido.tfg.domain.remediation.model.RemediationAction;
import com.spulido.tfg.domain.remediation.model.RemediationStrategy;
import com.spulido.tfg.domain.remediation.model.RemediationType;

public class RemediationStrategyRepositoryImpl implements RemediationStrategyRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public RemediationStrategyRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<RemediationStrategy> search(String cveId, String operatingSystem, String packageName,
            String remediationType, String action, Pageable pageable) {

        List<Criteria> criteria = new ArrayList<>();

        if (isNotBlank(cveId)) {
            criteria.add(Criteria.where("cveId").regex(Pattern.quote(cveId.trim()), "i"));
        }
        if (isNotBlank(operatingSystem)) {
            criteria.add(Criteria.where("operatingSystem").is(operatingSystem.trim()));
        }
        if (isNotBlank(packageName)) {
            criteria.add(Criteria.where("packageName").regex(Pattern.quote(packageName.trim()), "i"));
        }
        if (isNotBlank(remediationType)) {
            RemediationType type = parseEnum(RemediationType.class, remediationType);
            if (type == null) {
                return emptyPage(pageable);
            }
            criteria.add(Criteria.where("remediationType").is(type));
        }
        if (isNotBlank(action)) {
            RemediationAction remediationAction = parseEnum(RemediationAction.class, action);
            if (remediationAction == null) {
                return emptyPage(pageable);
            }
            criteria.add(Criteria.where("action").is(remediationAction));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, RemediationStrategy.class);
        List<RemediationStrategy> results = mongoTemplate.find(query.with(pageable), RemediationStrategy.class);

        return new PageImpl<>(results, pageable, total);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Page<RemediationStrategy> emptyPage(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
