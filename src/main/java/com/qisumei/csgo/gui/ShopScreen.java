package com.qisumei.csgo.gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 创建人：  @author SelfAbandonment
 * 创建时间: 2025-10-19 12:39
 */



public class ShopScreen extends Screen {



    public ShopScreen() {
        super(Component.literal("武器购买界面"));
    }

    @Override
    protected void init() {
        super.init();
        // 在这里添加界面元素（按钮、列表等）

        // 示例：添加关闭按钮（实际项目中需要更完善的按钮逻辑）
        this.addRenderableWidget(new Button.Builder(Component.literal("关闭"), (button) -> this.onClose())
                .pos(this.width / 2 - 50, this.height / 2 + 100)
                .size(100, 20)
                .build());
    }


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics,mouseX,mouseY,partialTick);

        // 绘制标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        // 购买界面不暂停游戏
        return false;
    }
}
