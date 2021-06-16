package com.rayworks.droidweekly;

import com.rayworks.droidweekly.dashboard.TestEvent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Method;

import timber.log.Timber;

@Aspect
public class EventAspect {

    private static final String POINTCUT_METHOD =
            "execution(@org.greenrobot.eventbus.Subscribe * *(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithSubscribe() {
    }

    @Around("methodAnnotatedWithSubscribe()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        Method method = methodSignature.getMethod();

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length == 1) {
            if (args[0] instanceof TestEvent) {
                TestEvent event = (TestEvent) args[0];
                Class<?> clazz = event.getClass();

                Timber.i(">>> consuming test event ( name : %s), removed now",
                        clazz.getSimpleName());
                EventBus.getDefault().removeStickyEvent(clazz);
                EventBus.getDefault().removeStickyEvent(event);
            }
        }

        return joinPoint.proceed();
    }
}
