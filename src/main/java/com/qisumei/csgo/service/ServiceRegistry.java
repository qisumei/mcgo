package com.qisumei.csgo.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务注册表 - 实现简单的服务定位器模式（Service Locator Pattern）。
 * 
 * <p>此类用于解耦模块之间的直接静态依赖，允许在运行时注册和获取服务实现。
 * 虽然服务定位器模式不如依赖注入（DI）框架优雅，但对于 Minecraft 模组来说
 * 是一个轻量级且实用的解决方案。</p>
 * 
 * <h3>设计模式：服务定位器（Service Locator）</h3>
 * <p>优点：</p>
 * <ul>
 *   <li>解耦：避免模块间的直接静态依赖</li>
 *   <li>灵活：运行时可以替换服务实现</li>
 *   <li>可测试：便于注入 mock 实现</li>
 *   <li>轻量：无需引入复杂的 DI 框架</li>
 * </ul>
 * 
 * <p>注意事项：</p>
 * <ul>
 *   <li>服务定位器模式可能隐藏依赖关系，建议谨慎使用</li>
 *   <li>对于大型项目，考虑使用成熟的 DI 框架（如 Guice）</li>
 *   <li>所有服务应在模组初始化阶段注册完成</li>
 * </ul>
 * 
 * <h3>线程安全性</h3>
 * <p>使用 ConcurrentHashMap 保证线程安全，支持并发读写。</p>
 * 
 * @see EconomyService
 * @see MatchService
 */
@SuppressWarnings("unused")
public final class ServiceRegistry {
    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    private ServiceRegistry() {
        // 私有构造函数防止实例化
    }

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
