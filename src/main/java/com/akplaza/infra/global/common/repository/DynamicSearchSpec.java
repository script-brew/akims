package com.akplaza.infra.global.common.repository;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicSearchSpec {

    /**
     * 🌟 프론트엔드에서 넘어온 검색 조건(Map)을 동적 WHERE 절로 변환하는 마법의 메서드
     */
    public static <T> Specification<T> searchConditions(Map<String, String> searchParams) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : searchParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // 1. 페이징 시스템 파라미터는 검색 조건에서 제외
                if (key.equals("page") || key.equals("size") || key.equals("sort")) {
                    continue;
                }

                // 2. 값이 존재하는 경우에만 동적으로 조건(Predicate) 추가
                if (value != null && !value.trim().isEmpty()) {
                    // .as(String.class)를 사용하여 Enum이나 숫자형 컬럼에도 안전하게 LIKE 검색 적용
                    predicates.add(builder.like(root.get(key).as(String.class), "%" + value.trim() + "%"));
                }
            }

            // 조립된 모든 조건들을 AND 로 묶어서 반환 (조건이 없으면 조건 없는 전체 조회)
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}