package com.qisumei.csgo.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易服务注册表，用于解耦模块之间的直接静态依赖。
 *
 * 改进点：
 * - 提供类型安全的获取（返回 Optional）以及抛出式获取方法。
 * - register 会在参数不合法时抛出异常，避免静默忽略错误。
 * - 增加 unregister/contains 方法以便在运行时管理生命周期。
 */
@SuppressWarnings("unused")
public final class ServiceRegistry {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private ServiceRegistry() {}

    /**
     * 注册服务实现；如果有错误参数会抛出 IllegalArgumentException。
     * 返回被替换的实现（如果有）以便调用方做进一步处理。
     */
    @SuppressWarnings("unchecked")
    public static <T> T register(Class<T> key, T implementation) {
        if (key == null) throw new IllegalArgumentException("Service key cannot be null");
        if (implementation == null) throw new IllegalArgumentException("Service implementation cannot be null");
        return (T) SERVICES.put(key, implementation);
    }

    /**
     * 普通获取，可能返回 null（保持向后兼容）。
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> key) {
        if (key == null) return null;
        return (T) SERVICES.get(key);
    }

    /**
     * 获取服务的 Optional 封装，更明确地表达可能缺失的语义。
     */
    public static <T> Optional<T> getOptional(Class<T> key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * 获取服务或抛出 IllegalStateException（当服务未注册时）。
     */
    public static <T> T getOrThrow(Class<T> key) {
        T svc = get(key);
        if (svc == null) throw new IllegalStateException("No service registered for " + key);
        return svc;
    }

    /**
     * 注销已注册的服务（如果存在）。
     */
    public static <T> void unregister(Class<T> key) {
        if (key == null) return;
        SERVICES.remove(key);
    }

    /**
     * 检查是否已注册指定服务。
     */
    public static boolean contains(Class<?> key) {
        return key != null && SERVICES.containsKey(key);
    }
}
