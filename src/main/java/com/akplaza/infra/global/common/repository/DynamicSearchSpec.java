package com.akplaza.infra.global.common.repository;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicSearchSpec {

    public static <T> Specification<T> searchConditions(Map<String, String> searchParams) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 1. 시스템 파라미터는 검색 조건에서 제외
                if (key.equals("page") || key.equals("size") || key.equals("sort")) {
                    continue;
                }

                // 2. 값이 존재하는 경우에만 처리
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        // 🛡️ 방어 로직 1: 엔티티에 해당 컬럼(key)이 존재하는지 시도하고, 없으면 예외를 무시함
                        predicates.add(builder.like(root.get(key).as(String.class), "%" + value.trim() + "%"));
                    } catch (IllegalArgumentException e) {
                        // 프론트엔드에서 엔티티에 없는 파라미터를 보냈을 경우 무시하고 다음 조건으로 넘어감
                        continue;
                    }
                }
            }

            // 🚨 핵심 해결 포인트: 조립된 조건이 하나도 없을 경우 null을 반환!
            // (Spring Data JPA에서 Specification이 null을 반환하면 WHERE 절 없이 전체를 깔끔하게 조회합니다)
            if (predicates.isEmpty()) {
                return null;
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}