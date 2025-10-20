package com.qisumei.csgo.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 武器购买界面的GUI屏幕。
 * <p>
 * 这个类是商店界面的一个基本实现。目前，它只显示一个标题和关闭按钮。
 * 未来的开发需要在此基础上添加武器分类、物品列表、购买按钮以及与服务器的交互逻辑。
 * </p>
 *
 * @author SelfAbandonment, Qisumei
 */
public class ShopScreen extends Screen {

    /**
     * 构造函数，设置屏幕的标题。
     */
    public ShopScreen() {
        super(Component.literal("武器购买界面"));
    }

    /**
     * 初始化屏幕中的所有UI组件（Widgets）。
     * 此方法在屏幕打开时被调用。
     */
    @Override
    protected void init() {
        super.init();
        // 在此处添加界面元素，例如武器分类按钮、物品列表等。

        // 添加一个居中的关闭按钮
        this.addRenderableWidget(new Button.Builder(Component.literal("关闭"), (button) -> this.onClose())
                .pos(this.width / 2 - 50, this.height - 40) // 放置在屏幕底部
                .size(100, 20)
                .build());
    }

    /**
     * 渲染屏幕内容。
     *
     * @param guiGraphics GUI图形上下文，用于所有绘制操作。
     * @param mouseX      鼠标的X坐标。
     * @param mouseY      鼠标的Y坐标。
     * @param partialTick 帧之间的部分tick，用于平滑动画。
     */
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染一个默认的半透明背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 在屏幕顶部中央绘制标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF); // 白色

        // 调用父类方法来渲染所有通过 addRenderableWidget 添加的组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 在此处可以添加额外的绘制逻辑，例如鼠标悬浮提示（Tooltips）。
    }

    /**
     * 决定此屏幕是否会暂停游戏。
     *
     * @return 返回 false，表示打开商店界面时，游戏世界会继续运行。
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
