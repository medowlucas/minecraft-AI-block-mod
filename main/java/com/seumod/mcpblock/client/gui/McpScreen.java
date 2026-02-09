package com.seumod.mcpblock.client.gui;

import com.seumod.mcpblock.client.McpClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.text.Text;

public class McpScreen extends Screen {
    private TextFieldWidget inputField;
    private ButtonWidget sendButton;
    private Text responseText = Text.literal("");
    private boolean thinking = false;

    private static McpScreen instance;

    protected McpScreen() {
        super(Text.literal("MCP Block AI"));
    }

    public static McpScreen getInstance() {
        if (instance == null) {
            instance = new McpScreen();
        }
        return instance;
    }

    @Override
    protected void init() {
        super.init();
        instance = this;

        inputField = new TextFieldWidget(
            this.textRenderer,
            this.width / 2 - 150,
            50,
            300,
            20,
            Text.literal("Input")
        );
        inputField.setMaxLength(500);
        inputField.setPlaceholder(Text.literal("Digite sua pergunta..."));
        this.addDrawableChild(inputField);

        sendButton = ButtonWidget.builder(
            Text.literal("Enviar"),
            btn -> sendRequest()
        ).dimensions(this.width / 2 - 50, 80, 100, 20).build();
        this.addDrawableChild(sendButton);
    }

    private void sendRequest() {
        String text = inputField.getText();
        if (text.isEmpty()) return;

        inputField.setText("");
        thinking = true;
        responseText = Text.literal("IA pensando...");
        McpClient.sendText(text);
    }

    public void setResponse(String response) {
        thinking = false;
        responseText = Text.literal(response);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("MCP Block AI"), this.width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, responseText, 10, 120, 0xAAAAAA);

        if (thinking) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("..."), this.width / 2 + 60, 82, 0xFFFF00);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(input) || inputField.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return inputField.charTyped(input);
    }
}
