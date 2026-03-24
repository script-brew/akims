package com.akplaza.infra.global.logging;

import java.util.Arrays;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String TRACE_ID = "traceId";

    // 1. Pointcut 정의: 어디에 로깅을 적용할 것인가?
    // com.akplaza.infra.domain 하위의 모든 Controller와 Service를 타겟으로 지정
    @Pointcut("execution(* com.akplaza.infra.domain..*Controller.*(..))")
    public void controllerPointcut() {}

    @Pointcut("execution(* com.akplaza.infra.domain..*Service.*(..))")
    public void servicePointcut() {}

    // 2. Around Advice: 대상 메서드의 실행 전/후/예외 발생 시점 제어
    @Around("controllerPointcut() || servicePointcut()")
    public Object logStepByStep(ProceedingJoinPoint joinPoint) throws Throwable {
        
        // MDC(Mapped Diagnostic Context)를 활용한 Trace ID 발급
        // 동일한 HTTP 요청이나 스레드 내에서 발생하는 로그들을 하나로 묶어 추적할 수 있게 해줍니다.
        boolean isNewTrace = false;
        if (MDC.get(TRACE_ID) == null) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
            isNewTrace = true;
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // [단계 1: 메서드 진입] - 어떤 파라미터를 들고 들어왔는지 기록 (DEBUG 레벨)
        log.debug("[START] {} -> {} | args: {}", className, methodName, Arrays.toString(args));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            // 실제 비즈니스 로직(Controller 또는 Service) 실행
            Object result = joinPoint.proceed();

            stopWatch.stop();
            long timeTaken = stopWatch.getTotalTimeMillis();

            // [단계 2: 메서드 완료] - 성공 여부 및 성능(실행 시간) 기록 (INFO 레벨)
            // 1초(1000ms) 이상 걸리면 WARN 레벨로 로깅하여 성능 지연 감지
            if (timeTaken > 1000) {
                log.warn("[SLOW] {} -> {} ({}ms)", className, methodName, timeTaken);
            } else {
                log.info("[END] {} -> {} ({}ms)", className, methodName, timeTaken);
            }

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            // [단계 3: 예외 발생] - 에러 원인과 발생 위치 기록 (ERROR 레벨)
            log.error("[ERROR] {} -> {} | Message: {}", className, methodName, e.getMessage());
            
            // 예외를 삼키지 않고 그대로 던져서, GlobalExceptionHandler가 처리할 수 있게 함
            throw e; 
            
        } finally {
            // 메모리 누수 방지를 위해 처음에 생성한 Trace ID 제거
            if (isNewTrace) {
                MDC.clear();
            }
        }
    }
}