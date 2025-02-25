package com.devcl.aetherbridge.feature;

/**
 * 功能接口
 * 所有功能模块都需要实现这个接口
 */
public interface Feature {
    /**
     * 启用功能
     * @throws Exception 启用过程中的异常
     */
    void enable() throws Exception;

    /**
     * 禁用功能
     * @throws Exception 禁用过程中的异常
     */
    void disable() throws Exception;

    /**
     * 检查功能是否已启用
     */
    boolean isEnabled();

    /**
     * 获取功能ID
     */
    String getId();
} 