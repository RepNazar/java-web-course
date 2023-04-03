package com.bobocode;

import com.bobocode.annotation.Trimmed;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This is processor class implements {@link BeanPostProcessor}, looks for a beans where method parameters are marked with
 * {@link Trimmed} annotation, creates proxy of them, overrides methods and trims all {@link String} arguments marked with
 * {@link Trimmed}. For example if there is a string " Java   " as an input parameter it has to be automatically trimmed to "Java"
 * if parameter is marked with {@link Trimmed} annotation.
 * <p>
 * <p>
 * Note! This bean is not marked as a {@link Component} to avoid automatic scanning, instead it should be created in
 * {@link StringTrimmingConfiguration} class which can be imported to a {@link Configuration} class by annotation
 * {@link EnableStringTrimming}
 */
public class TrimmedAnnotationBeanPostProcessor implements BeanPostProcessor {

    private Map<String, Class<?>> beansToProcess = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (Arrays.stream(bean.getClass().getDeclaredMethods())
                .flatMap(method -> Stream.of(method.getParameters()))
                .anyMatch(parameter -> parameter.isAnnotationPresent(Trimmed.class))) {
            beansToProcess.put(beanName, bean.getClass());
        }

        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beansToProcess.containsKey(beanName)) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(bean.getClass());
            enhancer.setCallback(trimInterceptor());
            return enhancer.create();
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    private MethodInterceptor trimInterceptor() {
        return (obj, method, args, proxy) -> proxy.invokeSuper(obj, trimFields(method, args));
    }

    private Object[] trimFields(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(Trimmed.class)) {
                if (args[i] instanceof String s) {
                    if (!s.isEmpty()) {
                        args[i] = s.trim();
                    }
                }
            }
        }
        return args;
    }

}
