package com.rayworks.droidweekly

import com.rayworks.droidweekly.dashboard.TestEvent
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.*

@Aspect
class EventAspect {
    companion object {
        const val POINTCUT_METHOD = "execution(@org.greenrobot.eventbus.Subscribe * *(..))"
    }


    @Pointcut(POINTCUT_METHOD)
    fun methodAnnotatedWithSubscribe() {
    }

    @Around("methodAnnotatedWithSubscribe()")
    @Throws(Throwable::class)
    fun weaveJoinPoint(joinPoint: ProceedingJoinPoint): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val className = methodSignature.declaringType.simpleName
        val methodName = methodSignature.name
        val method = methodSignature.method
        val args = joinPoint.args
        println(">>> ${Arrays.toString(args)}")

        if (args != null && args.size == 1) {
            if (args[0] is TestEvent) {
                val event: TestEvent = args[0] as TestEvent
                val clazz: Class<*> = event.javaClass
                Timber.i(">>> consuming sticky event ( name : %s ) in method : %s, removed now", clazz.simpleName, methodName)
                EventBus.getDefault().removeStickyEvent(clazz)
                EventBus.getDefault().removeStickyEvent(event)
            }
        }
        return joinPoint.proceed()
    }
}